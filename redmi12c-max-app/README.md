# Redmi 12C Max APK

Native Android helper for the existing `redmi12c-max` Termux/Wireless-ADB scripts.

## What the app does

- identifies Redmi 12C codename `earth`
- opens Xiaomi/HyperOS system update settings
- helps enable the Termux `RUN_COMMAND` bridge
- accepts Wireless-ADB pairing endpoint, pairing code and debug endpoint
- launches the GitHub-backed optimizer in Termux
- runs `optimize.sh --debloat --compile`
- can run `restore.sh`

The app does not bypass Android update signing, bootloader protections or package permissions. HyperOS OTA stays in Xiaomi's updater; privileged tuning runs through user-authorized Termux + Wireless ADB.

## First-time setup

1. Install current Termux.
2. Open the app and tap **Termux einmalig vorbereiten**.
3. Paste the copied command into Termux once. This sets `allow-external-apps=true`.
4. Grant the app the Android additional permission **Run commands in Termux environment**.
5. Enable **Developer options → Wireless debugging**.
6. Enter pair endpoint/code and debug endpoint, then tap **ADB koppeln + MAX optimieren**.

## Build

GitHub Actions builds a debug-signed installable APK named `Redmi12C-Max.apk`.
