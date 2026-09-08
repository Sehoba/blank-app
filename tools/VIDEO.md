# Mundbewegungs-Montage

Python 3.11+, FFmpeg, numpy==2.2.6 und opencv-python-headless==4.12.0.88.

```sh
python -m pip install numpy==2.2.6 opencv-python-headless==4.12.0.88
python tools/mouth_cut.py original.mp4 sprache.opus output.mp4
```

ZIP-Audio vorher entpacken. Alternativ nach Übernahme des Branches GitHub Actions → Mouth motion montage mit direkten HTTPS-Downloadlinks starten. ChatGPT-Anhänge werden nicht automatisch an GitHub übertragen. Das Repository ist öffentlich; persönliche Quelldateien nicht einchecken.

Der Algorithmus erkennt frontal sichtbare Gesichter, misst Bildänderungen im unteren Gesichtsbereich und sucht in Drei-Sekunden-Fenstern ähnliche Audio-Energieverläufe. Eine Wiederholungsstrafe bevorzugt unbenutzte Passagen. Die ursprüngliche Tonspur wird entfernt; die neue Aufnahme wird ohne Tempoänderung als AAC eingesetzt. Die Exportdauer folgt der dekodierten Aufnahme, mit Video-Frame-Rundung. Eine JSON-Datei dokumentiert Schnitte und Gesichtserkennungsquote.

Das ist eine experimentelle Rhythmus-Montage, keine Phonemerkennung oder KI-Neuberechnung von Lippen. Kopfbewegungen, Kameraschnitte, wechselnde Personen und Profile können das Matching täuschen. Bei weniger als fünf Prozent Gesichtserkennung bricht der Prozess ab. Ergebnisse visuell prüfen. Kein Android-Bedienelement ist mit diesem CLI/Actions-Modul verbunden.
