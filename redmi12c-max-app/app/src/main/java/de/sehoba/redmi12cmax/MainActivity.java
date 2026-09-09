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

        TextView title = text("Redmi 12C Max", 28, true);
        root.addView(title);

        TextView intro = text(
                "HyperOS-Update öffnen, Termux vorbereiten und die Redmi-12C-Optimierung aus GitHub starten. Kein Root, kein Bootloader-Unlock.",
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
                "Optional: Für die erste Wireless-ADB-Verbindung die Daten aus Entwickleroptionen → Wireless-Debugging eintragen.",
                13, false);
        hint.setPadding(0, dp(18), 0, dp(6));
        root.addView(hint);

        pairAddress = field("Pairing-Adresse, z. B. 192.168.1.25:37123", false);
        root.addView(pairAddress, matchWrapTopSmall());

        pairCode = field("6-stelliger Pairing-Code", true);
        root.addView(pairCode, matchWrapTopSmall());

        debugAddress = field("ADB-Adresse, z. B. 192.168.1.25:42891", false);
        root.addView(debugAddress, matchWrapTopSmall());

        Button auto = button("3. ADB koppeln + MAX optimieren");
        auto.setOnClickListener(v -> runFullInstall(true));
        root.addView(auto, matchWrapTop());

        Button optimize = button("Nur MAX optimieren (ADB bereits verbunden)");
        optimize.setOnClickListener(v -> runFullInstall(false));
        root.addView(optimize, matchWrapTopSmall());

        Button restore = button("Optimierung zurücksetzen");
        restore.setOnClickListener(v -> runRestore());
        root.addView(restore, matchWrapTopSmall());

        TextView footer = text(
                "Die APK darf Android-Sicherheitsgrenzen nicht umgehen. Firmware wird deshalb über Xiaomis System-Updater installiert; privilegierte Optimierungen laufen sichtbar über Termux + Wireless ADB.",
                12, false);
        footer.setPadding(0, dp(20), 0, 0);
        root.addView(footer);

        return scroll;
    }

    private void refreshDeviceStatus() {
        String device = getProp("ro.product.device");
        String model = getProp("ro.product.model");
        String build = getProp("ro.build.version.incremental");
        boolean termux = isPackageInstalled(TERMUX_PACKAGE);

        StringBuilder sb = new StringBuilder();
        sb.append("Modell: ").append(empty(model, android.os.Build.MODEL)).append('\n');
        sb.append("Codename: ").append(empty(device, "unbekannt")).append('\n');
        sb.append("Build: ").append(empty(build, android.os.Build.DISPLAY)).append('\n');
        sb.append("Termux: ").append(termux ? "installiert" : "nicht gefunden").append('\n');
        sb.append("RUN_COMMAND: ").append(hasTermuxPermission() ? "erlaubt" : "noch nicht erlaubt");

        if (!"earth".equals(device)) {
            sb.append("\n\n⚠ Dieses Projekt ist für Redmi 12C (earth) vorgesehen.");
        }
        status.setText(sb.toString());
    }

    private void openSystemUpdater() {
        try {
            startActivity(new Intent(Settings.ACTION_SYSTEM_UPDATE_SETTINGS));
        } catch (Exception first) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                toast("Systemeinstellungen konnten nicht geöffnet werden.");
            }
        }
    }

    private void prepareTermux() {
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            toast("Termux ist nicht installiert. Installiere zuerst die aktuelle Termux-Version.");
            return;
        }

        String setup = "mkdir -p ~/.termux && touch ~/.termux/termux.properties && " +
                "(grep -q '^allow-external-apps=' ~/.termux/termux.properties && " +
                "sed -i 's/^allow-external-apps=.*/allow-external-apps=true/' ~/.termux/termux.properties || " +
                "printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties) && termux-reload-settings";

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Redmi12C Max Termux Setup", setup));

        if (!hasTermuxPermission()) {
            requestPermissions(new String[]{TERMUX_PERMISSION}, REQ_TERMUX);
        }

        Intent launch = getPackageManager().getLaunchIntentForPackage(TERMUX_PACKAGE);
        if (launch != null) startActivity(launch);
        toast("Setup-Befehl kopiert. In Termux einmal einfügen und ausführen.");
    }

    private void runFullInstall(boolean pairFirst) {
        if (!preflight()) return;

        StringBuilder cmd = new StringBuilder();
        cmd.append("set -e; ");
        cmd.append("pkg update -y && pkg upgrade -y && pkg install -y git android-tools; ");

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

        cmd.append("cd \"$HOME\"; ");
        cmd.append("if [ -d blank-app/.git ]; then git -C blank-app pull --ff-only; ");
        cmd.append("else git clone --depth 1 https://github.com/Sehoba/blank-app.git; fi; ");
        cmd.append("cd \"$HOME/blank-app/redmi12c-max\"; ");
        cmd.append("chmod +x optimize.sh restore.sh; ");
        cmd.append("./optimize.sh --debloat --compile; ");
        cmd.append("echo; echo 'Redmi 12C Max abgeschlossen. Neustart empfohlen.'");

        runInTermux(cmd.toString(), "Redmi 12C MAX Optimierung");
    }

    private void runRestore() {
        if (!preflight()) return;
        String cmd = "set -e; cd \"$HOME/blank-app/redmi12c-max\" && chmod +x restore.sh && ./restore.sh";
        runInTermux(cmd, "Redmi 12C MAX Restore");
    }

    private boolean preflight() {
        String device = getProp("ro.product.device");
        if (!"earth".equals(device)) {
            toast("Abbruch: Gerät ist nicht als Redmi 12C / earth erkannt.");
            return false;
        }
        if (!isPackageInstalled(TERMUX_PACKAGE)) {
            toast("Termux ist nicht installiert.");
            return false;
        }
        if (!hasTermuxPermission()) {
            requestPermissions(new String[]{TERMUX_PERMISSION}, REQ_TERMUX);
            toast("Erlaube zuerst 'Befehle in Termux ausführen'. Danach erneut starten.");
            return false;
        }
        return true;
    }

    private void runInTermux(String command, String label) {
        Intent intent = new Intent();
        intent.setClassName(TERMUX_PACKAGE, TERMUX_SERVICE);
        intent.setAction("com.termux.RUN_COMMAND");
        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-lc", command});
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        intent.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", label);
        intent.putExtra("com.termux.RUN_COMMAND_COMMAND_DESCRIPTION", "Redmi 12C HyperOS/ADB Optimierung");

        try {
            startService(intent);
            toast("Termux-Auftrag gestartet.");
        } catch (SecurityException e) {
            toast("RUN_COMMAND-Berechtigung fehlt oder allow-external-apps ist noch nicht aktiv.");
        } catch (Exception e) {
            toast("Termux-Auftrag konnte nicht gestartet werden: " + e.getClass().getSimpleName());
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
            Process p = new ProcessBuilder("getprop", key).redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                return line == null ? "" : line.trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String empty(String value, String fallback) {
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
