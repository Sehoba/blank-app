# Redmi12C-Max

Termux + Wireless ADB Optimierung für Xiaomi Redmi 12C (`earth`) mit HyperOS EEA/EUXM.

## Ziel

- Backup vor Änderungen
- Animationen auf 0,5x
- Cache trimmen
- optional konservatives Xiaomi-Debloat
- optional ART speed-profile Kompilierung
- Restore-Skript

Kein Root und kein Bootloader-Unlock erforderlich.

## 1. HyperOS zuerst aktualisieren

Auf dem Redmi 12C unter **Einstellungen -> Über das Telefon -> HyperOS -> Nach Updates suchen** die aktuelle EUXM-Firmware installieren. Nicht zwischen EUXM/MIXM/INXM wechseln.

## 2. Termux vorbereiten

```bash
pkg update -y && pkg upgrade -y
pkg install android-tools git -y
```

Auf Android **Entwickleroptionen -> Wireless-Debugging** aktivieren.

Dann koppeln und verbinden:

```bash
adb pair IP:PAIRING_PORT
adb connect IP:ADB_PORT
adb devices
```

## 3. Projekt holen

```bash
git clone https://github.com/Sehoba/blank-app.git
cd blank-app/redmi12c-max
chmod +x optimize.sh restore.sh
```

## 4. Master-One-Liner für Termux

Dieser Befehl aktualisiert Termux, installiert Git + ADB, klont oder aktualisiert das Projekt und startet anschließend die maximale konservative Optimierung:

```bash
pkg update -y && pkg upgrade -y && pkg install -y git android-tools && cd "$HOME" && { [ -d blank-app/.git ] && git -C blank-app pull --ff-only || git clone https://github.com/Sehoba/blank-app.git; } && cd "$HOME/blank-app/redmi12c-max" && chmod +x optimize.sh restore.sh && echo "=== Redmi 12C MAX ===" && adb devices && ./optimize.sh --debloat --compile
```

Beim ersten Mal muss Wireless ADB vorher gekoppelt und verbunden werden:

```bash
adb pair IP:PAIRING_PORT
adb connect IP:ADB_PORT
```

## Sichere Basisoptimierung

```bash
./optimize.sh
```

## Optional: konservatives Debloat

Deaktiviert nur diese drei optionalen Xiaomi-Pakete, falls vorhanden:

- `com.miui.msa.global`
- `com.miui.analytics`
- `com.xiaomi.mipicks`

```bash
./optimize.sh --debloat
```

## Optional: ART App-Optimierung

```bash
./optimize.sh --compile
```

Beides zusammen:

```bash
./optimize.sh --debloat --compile
```

## Wiederherstellen

```bash
./restore.sh
```

## Hinweise

Das Skript prüft den Geräte-Codenamen `earth` und bricht auf anderen Geräten ab. Kritische HyperOS-Pakete wie Security Center, Updater, Launcher, Telefon, Berechtigungsverwaltung und Powerkeeper werden absichtlich nicht deaktiviert.

Backups landen unter:

```text
~/redmi12c-max/backup/YYYYMMDD-HHMMSS/
```
