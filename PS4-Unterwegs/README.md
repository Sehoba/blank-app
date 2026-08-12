# PS4 Unterwegs – Android

Leichte Begleit-App für die offizielle PS Remote Play-App (`com.playstation.remoteplay`). Sie ersetzt Remote Play nicht, sondern macht den Start unterwegs schneller und berechenbarer.

## Funktionen
- Direkter Start von PS Remote Play oder Play-Store-Fallback.
- Live-Erkennung von WLAN, Mobilfunk und Verbindungswechseln.
- Mehrfachmessung der Internet-Reaktionszeit mit Median statt eines einzelnen Ausreißers.
- Automatische Empfehlung für **Qualität**, **Ausgewogen** oder **Latenz**.
- Gamepad-Erkennung über Android InputDevice.
- Persistenter PS4-Startcheck und Datenverbrauchsrechner.
- Offline nutzbare Begleitoberfläche ohne Konto und ohne Tracker.

## Performance-Build
Der Release-Build ist minifiziert und ressourcenoptimiert. Für einfache Testinstallationen wird er mit Androids Debug-Signatur signiert. Für Play-Store-Veröffentlichungen muss später ein eigener Release-Key verwendet werden.

## GitHub Actions
Der Workflow im Repository baut automatisch eine optimierte Release-APK und lädt zusätzlich eine SHA-256-Prüfsumme als Artifact hoch.

> PS4-Spiele selbst werden nicht auf Android gespeichert. Das eigentliche Spielen benötigt weiterhin eine Remote-Play-Verbindung zur PS4.
