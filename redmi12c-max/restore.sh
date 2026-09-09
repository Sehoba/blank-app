#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

need() { command -v "$1" >/dev/null 2>&1 || { echo "Fehlt: $1"; exit 1; }; }
need adb

if ! adb get-state >/dev/null 2>&1; then
  echo "Kein ADB-Gerät verbunden."
  exit 1
fi

CODENAME="$(adb shell getprop ro.product.device | tr -d '\r')"
if [ "$CODENAME" != "earth" ]; then
  echo "Abbruch: Dieses Skript ist nur für Redmi 12C (earth) gedacht."
  exit 1
fi

# Standard-Animationen zurücksetzen
adb shell settings delete global window_animation_scale || true
adb shell settings delete global transition_animation_scale || true
adb shell settings delete global animator_duration_scale || true

# Nur Pakete wieder aktivieren, die dieses Projekt optional deaktiviert
for pkg in com.miui.msa.global com.miui.analytics com.xiaomi.mipicks; do
  if adb shell pm list packages "$pkg" | grep -q "$pkg"; then
    echo "Aktiviere: $pkg"
    adb shell pm enable --user 0 "$pkg" >/dev/null 2>&1 || true
  fi
done

echo "Wiederherstellung abgeschlossen. Neustart empfohlen: adb reboot"
