# Auto-Tuning Tool

Kostenloses lokales FFmpeg-Werkzeug für Kurzvideos. Es normalisiert die Sprachlautheit, begrenzt Spitzen, filtert tiefe Störgeräusche und erzeugt eine kompatible 9:16-MP4.

## Nutzung

```bash
python3 auto_tune.py input.mp4 output.mp4
```

Standard: 576×1024, Ziel-Lautheit −14 LUFS, AAC 160 kbit/s, H.264.

Das Tool erzeugt keine neue Stimme und synchronisiert keine Lippen. Dafür braucht man eine separate Sprach- bzw. Lip-Sync-Engine. Es lädt keine Dateien hoch.
