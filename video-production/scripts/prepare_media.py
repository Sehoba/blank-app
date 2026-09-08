from __future__ import annotations

import argparse
import subprocess
from pathlib import Path


def run(*args: str) -> None:
    subprocess.run(args, check=True)


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare video and external voice track for lip-sync production")
    parser.add_argument("--video", default="input/video.mp4")
    parser.add_argument("--audio", default="input/audio.opus")
    parser.add_argument("--out", default="output")
    args = parser.parse_args()

    video = Path(args.video)
    audio = Path(args.audio)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    if not video.exists():
        raise SystemExit(f"Missing video: {video}")
    if not audio.exists():
        raise SystemExit(f"Missing audio: {audio}")

    normalized_audio = out / "voice_normalized.wav"
    normalized_video = out / "video_normalized.mp4"
    muxed = out / "video_with_voice.mp4"

    run(
        "ffmpeg", "-y", "-i", str(audio),
        "-af", "loudnorm=I=-16:LRA=11:TP=-1.5",
        "-ar", "48000", "-ac", "1",
        str(normalized_audio),
    )

    run(
        "ffmpeg", "-y", "-i", str(video),
        "-c:v", "libx264", "-preset", "medium", "-crf", "18",
        "-pix_fmt", "yuv420p", "-an",
        str(normalized_video),
    )

    run(
        "ffmpeg", "-y",
        "-i", str(normalized_video),
        "-i", str(normalized_audio),
        "-map", "0:v:0", "-map", "1:a:0",
        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
        "-shortest",
        str(muxed),
    )

    print(f"Prepared: {muxed}")


if __name__ == "__main__":
    main()
