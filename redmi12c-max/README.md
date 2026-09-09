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

Solange das Projekt im Branch `redmi12c-max` von `Sehoba/blank-app` liegt:

```bash
git clone -b redmi12c-max https://github.com/Sehoba/blank-app.git
cd blank-app/redmi12c-max
chmod +x optimize.sh restore.sh
```

## 4. Sichere Basisoptimierung

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
