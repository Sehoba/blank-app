# Video Production Project

Eigenständiges Projekt für die Verarbeitung von Video + Sprachaufnahme.

## Ziel

- Eingabevideo verarbeiten
- externe Sprachaufnahme als Tonspur übernehmen
- Audio normalisieren und synchronisieren
- vorbereitete Schnittstelle für KI-Lippensync
- fertiges MP4 als GitHub-Actions-Artefakt ausgeben

## Struktur

- `input/video.mp4` – Quellvideo
- `input/audio.opus` – Sprachaufnahme
- `output/` – Render-Ergebnisse
- `scripts/prepare_media.py` – Medienvorbereitung
- `.github/workflows/video-production.yml` – automatischer Render-Workflow

## Aktueller Ablauf

1. Audio wird nach WAV konvertiert und normalisiert.
2. Video wird für die weitere Verarbeitung vereinheitlicht.
3. Audio wird auf das Video gemuxt.
4. Die Projektstruktur ist für einen späteren Wav2Lip-/MuseTalk-Schritt vorbereitet.

Hinweis: Ein echtes KI-Lippensync benötigt zusätzlich ein passendes Modell bzw. einen externen GPU-Runner. Der Workflow trennt deshalb bereits Medienvorbereitung und Lippensync-Stufe sauber voneinander.
