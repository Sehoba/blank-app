package de.sehoba.redmi12cmax;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_PERMISSION = "com.termux.permission.RUN_COMMAND";
    private static final String TERMUX_SERVICE = "com.termux.app.RunCommandService";
    private static final String SYSTEM_UPDATE_ACTION = "android.settings.SYSTEM_UPDATE_SETTINGS";
    private static final int REQ_TERMUX = 1201;

    private static final Pattern ENDPOINT = Pattern.compile("^(?:\\d{1,3}\\.){3}\\d{1,3}:\\d{1,5}$");
    private static final Pattern PAIR_CODE = Pattern.compile("^\\d{6}$");

    private TextView status;
    private EditText pairAddress;
    private EditText pairCode;
    private EditText debugAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refreshDeviceStatus();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(32));
        root.setBackgroundColor(Color.rgb(245, 245, 245));
        scroll.addView(root);

        root.addView(text("Redmi 12C Max", 28, true));

        TextView intro = text(
                "HyperOS aktualisieren, Termux vorbereiten und die Redmi-12C-Optimierung per Wireless ADB starten.",
                15, false);
        intro.setPadding(0, dp(8), 0, dp(14));
        root.addView(intro);

        status = text("Gerät wird geprüft …", 14, false);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        status.setBackgroundColor(Color.WHITE);
        root.addView(status, matchWrap());

        Button ota = button("1. HyperOS-Systemupdate öffnen");
        ota.setOnClickListener(v -> openSystemUpdater());
        root.addView(ota, matchWrapTop());

        Button setup = button("2. Termux einmalig vorbereiten");
        setup.setOnClickListener(v -> prepareTermux());
        root.addView(setup, matchWrapTop());

        TextView hint = text(
                "Für die erste Verbindung: Entwickleroptionen → Wireless-Debugging → Gerät mit Kopplungscode koppeln.",
                13, false);
        hint.setPadding(0, dp(18), 0, dp(6));
        root.addView(hint);

        pairAddress = field("Pairing-Adresse, z. B. 192.168.1.25:37123", false);
        pairCode = field("6-stelliger Pairing-Code", true);
        debugAddress = field("ADB-Adresse, z. B. 192.168.1.25:42891", false);
        root.addView(pairAddress, matchWrapTopSmall());
        root.addView(pairCode, matchWrapTopSmall());
        root.addView(debugAddress, matchWrapTopSmall());

        Button auto = button("3. ADB koppeln + MAX optimieren");
        auto.setOnClickListener(v -> runFullInstall(true));
        root.addView(auto, matchWrapTop());

        Button optimize = button("Nur MAX optimieren (ADB schon verbunden)");
        optimize.setOnClickListener(v -> runFullInstall(false));
        root.addView(optimize, matchWrapTopSmall());

        Button restore = button("Optimierung zurücksetzen");
        restore.setOnClickListener(v -> runRestore());
        root.addView(restore, matchWrapTopSmall());

        TextView footer = text(
                "Kein Root und kein Bootloader-Unlock. Die Firmware bleibt im Xiaomi-System-Updater; ADB-Optimierungen laufen über deine freigegebene Termux-Sitzung.",
                12, false);
        footer.setPadding(0, dp(20), 0, 0);
        root.addView(footer);
        return scroll;
    }

    private void refreshDeviceStatus() {
        String device = getProp("ro.product.device");
        String model = getProp("ro.product.model");
        String build = getProp("ro.build.version.incremental");

        StringBuilder sb = new StringBuilder();
        sb.append("Modell: ").append(empty(model, android.os.Build.MODEL)).append('\n');
        sb.append("Codename: ").append(empty(device, "unbekannt")).append('\n');
        sb.append("Build: ").append(empty(build, android.os.Build.DISPLAY)).append('\n');
        sb.append("Termux: ").append(isPackageInstalled(TERMUX_PACKAGE) ? "installiert" : "nicht gefunden").append('\n');
        sb.append("RUN_COMMAND: ").append(hasTermuxPermission() ? "erlaubt" : "noch nicht erlaubt");
        if (!"earth".equals(device)) {
            sb.append("\n\n⚠ Optimierung wird nur auf Redmi 12C / earth gestartet.");
        }
        status.setText(sb.toString());
    }

    private void openSystemUpdater() {
        Intent update = new Intent(SYSTEM_UPDATE_ACTION);
        try {
            if (update.resolveActivity(getPackageManager()) != null) {
                startActivity(update);
            } else {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                toast("Direkter HyperOS-Updater nicht auflösbar. Systemeinstellungen wurden geöffnet.");
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast("Systemeinstellungen konnten nicht geöffnet werden.");
            }
        }
    }

    private void prepareTermux() {
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            toast("Termux ist nicht installiert.");
            return;
        }

        String setup = "mkdir -p ~/.termux && touch ~/.termux/termux.properties && " +
                "(grep -q '^allow-external-apps=' ~/.termux/termux.properties && " +
                "sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' ~/.termux/termux.properties || " +
                "printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties) && termux-reload-settings";

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Redmi12C Max Setup", setup));

        if (!hasTermuxPermission()) {
            requestPermissions(new String[]{TERMUX_PERMISSION}, REQ_TERMUX);
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(TERMUX_PACKAGE);
        if (launch != null) startActivity(launch);
        toast("Termux-Setup ist kopiert. Einmal in Termux einfügen und ausführen.");
    }

    private void runFullInstall(boolean pairFirst) {
        if (!preflight()) return;

        StringBuilder cmd = new StringBuilder();
        cmd.append("set -e; pkg update -y && pkg upgrade -y && pkg install -y git android-tools; ");

        if (pairFirst) {
            String pair = pairAddress.getText().toString().trim();
            String code = pairCode.getText().toString().trim();
            String debug = debugAddress.getText().toString().trim();
            if (!ENDPOINT.matcher(pair).matches() || !PAIR_CODE.matcher(code).matches() || !ENDPOINT.matcher(debug).matches()) {
                toast("Pairing-Adresse, 6-stelligen Code und ADB-Adresse korrekt eintragen.");
                return;
            }
            cmd.append("printf '%s\\n' '").append(code).append("' | adb pair '").append(pair).append("'; ");
            cmd.append("adb connect '").append(debug).append("'; ");
        }

        cmd.append("adb get-state >/dev/null; cd \"$HOME\"; ");
        cmd.append("if [ -d blank-app/.git ]; then git -C blank-app pull --ff-only; ");
        cmd.append("else git clone --depth 1 https://github.com/Sehoba/blank-app.git; fi; ");
        cmd.append("cd \"$HOME/blank-app/redmi12c-max\"; chmod +x optimize.sh restore.sh; ");
        cmd.append("./optimize.sh --debloat --compile; echo; echo 'Redmi 12C Max abgeschlossen.'");

        runInTermux(cmd.toString(), "Redmi 12C MAX");
    }

    private void runRestore() {
        if (!preflight()) return;
        runInTermux(
                "set -e; cd \"$HOME/blank-app/redmi12c-max\" && chmod +x restore.sh && ./restore.sh",
                "Redmi 12C Restore");
    }

    private boolean preflight() {
        if (!"earth".equals(getProp("ro.product.device"))) {
            toast("Abbruch: Das Gerät ist nicht Redmi 12C / earth.");
            return false;
        }
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            toast("Termux ist nicht installiert.");
            return false;
        }
        if (!hasTermuxPermission()) {
            requestPermissions(new String[]{TERMUX_PERMISSION}, REQ_TERMUX);
            toast("RUN_COMMAND-Berechtigung erlauben und danach erneut starten.");
            return false;
        }
        return true;
    }

    private void runInTermux(String command, String label) {
        Intent intent = new Intent("com.termux.RUN_COMMAND");
        intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-lc", command});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        try {
            startService(intent);
            toast(label + " wurde in Termux gestartet.");
        } catch (SecurityException e) {
            toast("Termux RUN_COMMAND ist noch nicht freigegeben.");
        } catch (Exception e) {
            toast("Termux konnte nicht gestartet werden: " + e.getClass().getSimpleName());
        }
    }

    private boolean hasTermuxPermission() {
        return checkSelfPermission(TERMUX_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isPackageInstalled(String name) {
        try {
            getPackageManager().getPackageInfo(name, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String getProp(String key) {
        try {
            Process p = new ProcessBuilder("/system/bin/getprop", key).redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                return line == null ? "" : line.trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static String empty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(Color.rgb(25, 25, 25));
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return v;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    private EditText field(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(Color.WHITE);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchWrapTop() {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(14);
        return p;
    }

    private LinearLayout.LayoutParams matchWrapTopSmall() {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(8);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_TERMUX) refreshDeviceStatus();
    }
}
