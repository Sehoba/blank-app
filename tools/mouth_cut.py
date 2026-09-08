#!/usr/bin/env python3
"""Match visible lower-face motion to voice energy. Not phoneme-level lip synthesis."""
import argparse
import json
import math
from pathlib import Path
import subprocess
import tempfile

import cv2
import numpy as np

HZ = 10

def run(args):
    subprocess.run(args, check=True)

def normalized(a):
    scale = np.percentile(a, 95) if len(a) else 0
    return np.clip(a / max(float(scale), 1e-6), 0, 1)

def choose(motion, visible, energy):
    if len(motion) < 2 * HZ:
        raise ValueError("Video too short")
    if np.mean(visible) < 0.05:
        raise ValueError("No reliable faces detected; refusing an unverified montage")
    used = np.zeros(len(motion))
    cuts = []
    for offset in range(0, len(energy), 3 * HZ):
        target = energy[offset:offset + 3 * HZ]
        n = len(target)
        best = None
        for start in range(0, len(motion) - n + 1, 2):
            score = np.mean((motion[start:start+n] - target)**2)
            score += 1.5 * (1 - np.mean(visible[start:start+n]))
            score += 0.5 * np.mean(used[start:start+n])
            if best is None or score < best[0]:
                best = (float(score), start)
        if best is None:
            raise ValueError("Source video shorter than a target segment")
        score, start = best
        used[start:start+n] += 1
        cuts.append(dict(start=start / HZ, duration=n / HZ, score=score))
    return cuts

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("video")
    parser.add_argument("audio", help="Extract OPUS from ZIP first")
    parser.add_argument("output")
    args = parser.parse_args()
    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        pcm = root / "voice.f32"
        run(["ffmpeg", "-v", "error", "-y", "-i", args.audio, "-vn",
             "-ar", "16000", "-ac", "1", "-f", "f32le", str(pcm)])
        samples = np.fromfile(pcm, dtype=np.float32)
        if not len(samples):
            raise ValueError("Empty audio")
        duration = len(samples) / 16000
        padded = np.pad(samples, (0, (-len(samples)) % 1600))
        energy = normalized(np.sqrt(np.mean(padded.reshape(-1, 1600)**2, axis=1)))
        cap = cv2.VideoCapture(args.video)
        fps = cap.get(cv2.CAP_PROP_FPS)
        if not cap.isOpened() or fps <= 0:
            raise ValueError("Cannot decode video")
        detector = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
        motion, visible = [], []
        previous = None
        frame_index = 0
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            if frame_index / fps + 1e-6 >= len(motion) / HZ:
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                gray = cv2.resize(gray, (480, max(1, round(gray.shape[0] * 480 / gray.shape[1]))))
                faces = detector.detectMultiScale(gray, 1.1, 4, minSize=(35, 35))
                value = 0.0
                found = len(faces) > 0
                if found:
                    x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
                    roi = gray[y + int(h * .6):y + int(h * .92), x + int(w * .2):x + int(w * .8)]
                    roi = cv2.resize(roi, (64, 32))
                    if previous is not None:
                        value = float(np.mean(cv2.absdiff(roi, previous)))
                    previous = roi
                else:
                    previous = None
                motion.append(value)
                visible.append(float(found))
            frame_index += 1
        cap.release()
        cuts = choose(normalized(np.array(motion)), np.array(visible), energy)
        cuts[-1]["duration"] -= len(energy) / HZ - duration
        parts = []
        for i, cut in enumerate(cuts):
            part = root / ("part_%04d.mp4" % i)
            run(["ffmpeg", "-v", "error", "-y", "-ss", str(cut["start"]), "-i", args.video,
                 "-t", str(cut["duration"]), "-an", "-vf", "fps=30,setsar=1",
                 "-c:v", "libx264", "-preset", "fast", "-crf", "19", "-pix_fmt", "yuv420p", str(part)])
            parts.append("file '" + part.as_posix() + "'")
        listing = root / "concat.txt"
        listing.write_text("\n".join(parts))
        run(["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(listing),
             "-i", args.audio, "-map", "0:v:0", "-map", "1:a:0", "-t", str(duration),
             "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart", str(output)])
        output.with_suffix(".cuts.json").write_text(json.dumps({
            "method": "lower-face-motion versus audio-energy; not phoneme lip-sync",
            "audio_duration": duration, "face_detection_fraction": float(np.mean(visible)),
            "cuts": cuts}, indent=2))
        print(output)

if __name__ == "__main__":
    main()
