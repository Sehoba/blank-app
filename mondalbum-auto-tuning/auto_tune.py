#!/usr/bin/env python3
"""Kostenloses FFmpeg-Auto-Tuning für Sprach-/Kurzvideos.
Optimiert Lautheit, entfernt extreme Spitzen, reduziert Rauschen und schreibt eine
mobile MP4 mit AAC-Audio. Keine Cloud-API, keine Voice-Erzeugung, kein Klonen.
"""
import argparse, json, shutil, subprocess, sys
from pathlib import Path

def run(cmd):
    print('+', ' '.join(map(str, cmd)))
    subprocess.run(cmd, check=True)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('input', type=Path)
    ap.add_argument('output', type=Path)
    ap.add_argument('--loudness', type=float, default=-14.0, help='Ziel-LUFS, Standard -14')
    ap.add_argument('--width', type=int, default=576)
    ap.add_argument('--height', type=int, default=1024)
    args=ap.parse_args()
    if not shutil.which('ffmpeg'): raise SystemExit('ffmpeg fehlt')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    vf=(f"scale={args.width}:{args.height}:force_original_aspect_ratio=decrease,"
        f"pad={args.width}:{args.height}:(ow-iw)/2:(oh-ih)/2:black,format=yuv420p")
    af=(f"highpass=f=70,lowpass=f=14500,afftdn=nf=-25dB,"
        f"loudnorm=I={args.loudness}:LRA=7:TP=-1.5,alimiter=limit=0.95")
    run(['ffmpeg','-hide_banner','-y','-i',str(args.input),'-vf',vf,'-af',af,
         '-c:v','libx264','-preset','medium','-crf','20','-movflags','+faststart',
         '-c:a','aac','-b:a','160k','-ar','48000',str(args.output)])
    print(json.dumps({'input':str(args.input),'output':str(args.output),'target_lufs':args.loudness}))
if __name__=='__main__': main()
