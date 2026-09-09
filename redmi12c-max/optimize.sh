#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

PROJECT="$HOME/redmi12c-max"
BACKUP="$PROJECT/backup/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP"

need() { command -v "$1" >/dev/null 2>&1 || { echo "Fehlt: $1"; exit 1; }; }
need adb

if ! adb get-state >/dev/null 2>&1; then
  echo "Kein ADB-Gerät verbunden. Erst adb pair / adb connect ausführen."
  exit 1
fi

CODENAME="$(adb shell getprop ro.product.device | tr -d '\r')"
BUILD="$(adb shell getprop ro.build.version.incremental | tr -d '\r')"
MODEL="$(adb shell getprop ro.product.model | tr -d '\r')"

echo "Gerät: $MODEL | Codename: $CODENAME | Build: $BUILD"
if [ "$CODENAME" != "earth" ]; then
  echo "Abbruch: Dieses Skript ist nur für Redmi 12C (earth) gedacht."
  exit 1
fi

if [[ "$BUILD" != *"EUXM"* ]]; then
  echo "Hinweis: Firmware ist nicht als EUXM erkannt. Keine regionsspezifischen Änderungen werden durchgeführt."
fi

# Backup vor jeder Änderung
adb shell settings list global > "$BACKUP/settings-global.txt"
adb shell settings list secure > "$BACKUP/settings-secure.txt"
adb shell settings list system > "$BACKUP/settings-system.txt"
adb shell pm list packages -d > "$BACKUP/packages-disabled.txt"
adb shell pm list packages > "$BACKUP/packages-all.txt"
adb shell getprop > "$BACKUP/getprop.txt"
adb shell dumpsys meminfo > "$BACKUP/memory-before.txt"
adb shell df -h > "$BACKUP/storage-before.txt"

# Sichere UI-Optimierung
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5

# Cache bereinigen, keine App-Daten löschen
adb shell pm trim-caches 2G >/dev/null 2>&1 || true

# Optionale, konservative Xiaomi-Debloat-Liste
DEBLOAT=0
COMPILE=0
for arg in "$@"; do
  [ "$arg" = "--debloat" ] && DEBLOAT=1
  [ "$arg" = "--compile" ] && COMPILE=1
done

if [ "$DEBLOAT" -eq 1 ]; then
  printf '%s\n' \
    com.miui.msa.global \
    com.miui.analytics \
    com.xiaomi.mipicks > "$BACKUP/disabled-by-redmi12c-max.txt"

  while read -r pkg; do
    if adb shell pm list packages "$pkg" | grep -q "$pkg"; then
      echo "Deaktiviere optionales Paket: $pkg"
      adb shell pm disable-user --user 0 "$pkg" >/dev/null 2>&1 || true
    fi
  done < "$BACKUP/disabled-by-redmi12c-max.txt"
fi

if [ "$COMPILE" -eq 1 ]; then
  echo "ART speed-profile Optimierung läuft. Das kann einige Minuten dauern..."
  adb shell cmd package compile -m speed-profile -a >/dev/null 2>&1 || true
fi

adb shell dumpsys meminfo > "$BACKUP/memory-after.txt"
adb shell df -h > "$BACKUP/storage-after.txt"

echo
echo "Fertig. Backup: $BACKUP"
echo "Animationen: 0.5x"
[ "$DEBLOAT" -eq 1 ] && echo "Konservatives Debloat: aktiviert"
[ "$COMPILE" -eq 1 ] && echo "ART speed-profile: ausgeführt"
echo "Neustart empfohlen: adb reboot"
