#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo '=== REDMI 12C MASTER ==='

pkg update -y
pkg upgrade -y
pkg install -y git android-tools wget curl

MODEL="$(getprop ro.product.model 2>/dev/null || true)"
DEVICE="$(getprop ro.product.device 2>/dev/null || true)"
BUILD="$(getprop ro.build.version.incremental 2>/dev/null || true)"

echo "Modell: $MODEL"
echo "Codename: $DEVICE"
echo "Build: $BUILD"

if [ "$DEVICE" != "earth" ]; then
  echo 'ABBRUCH: Dieses Skript ist nur für Redmi 12C (earth).'
  exit 1
fi

echo '✓ Redmi 12C erkannt'

cd "$HOME"
if [ -d blank-app/.git ]; then
  git -C blank-app pull --ff-only
else
  git clone --depth 1 https://github.com/Sehoba/blank-app.git
fi

cd "$HOME/blank-app/redmi12c-max"
chmod +x optimize.sh restore.sh master-redmi12c.sh

if ! adb devices | awk 'NR>1 && $2=="device" {ok=1} END {exit !ok}'; then
  echo
  echo 'Wireless ADB ist noch nicht verbunden.'
  echo 'Aktiviere Entwickleroptionen -> Wireless-Debugging und führe danach aus:'
  echo '  adb pair IP:PAIRING_PORT'
  echo '  adb connect IP:ADB_PORT'
  echo 'Danach dieses Master-Skript erneut starten.'
  exit 2
fi

./optimize.sh --debloat --compile

echo
 echo '✓ Redmi 12C Optimierung abgeschlossen.'
echo 'Hinweis: Ein Fastboot-Selbstflash ist aus laufendem Termux technisch nicht möglich.'
