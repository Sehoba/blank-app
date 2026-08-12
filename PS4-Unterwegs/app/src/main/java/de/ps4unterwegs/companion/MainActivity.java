package de.ps4unterwegs.companion;

import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.input.InputManager;
import android.net.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements InputManager.InputDeviceListener {
    private static final String REMOTE_PACKAGE = "com.playstation.remoteplay";
    private static final String TEST_URL = "https://www.playstation.com/";

    private LinearLayout root, metrics, checklist;
    private TextView statusTitle, statusText, netType, latency, controller, profile, dataText, checkCount;
    private SharedPreferences prefs;
    private ConnectivityManager connectivityManager;
    private InputManager inputManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean testRunning = new AtomicBoolean(false);
    private long lastLatency = -1;

    private final int blue = Color.rgb(106,167,255), panel = Color.rgb(16,23,37), panel2 = Color.rgb(21,31,49),
            text = Color.rgb(243,247,255), muted = Color.rgb(158,171,192), line = Color.rgb(39,52,75),
            green = Color.rgb(88,214,141), yellow = Color.rgb(245,196,81), red = Color.rgb(255,107,107);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("ps4u", MODE_PRIVATE);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        buildUi();
        inputManager.registerInputDeviceListener(this, null);
        registerNetworkWatcher();
        updateAll();
        scheduleLatencyTest(150);
    }

    @Override protected void onDestroy() {
        try { inputManager.unregisterInputDeviceListener(this); } catch (Exception ignored) {}
        try { if (networkCallback != null) connectivityManager.unregisterNetworkCallback(networkCallback); } catch (Exception ignored) {}
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onInputDeviceAdded(int id) { updateController(); }
    @Override public void onInputDeviceRemoved(int id) { updateController(); }
    @Override public void onInputDeviceChanged(int id) { updateController(); }

    private void registerNetworkWatcher() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { networkChanged(); }
            @Override public void onLost(Network network) { networkChanged(); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities nc) { networkChanged(); }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private void networkChanged() {
        mainHandler.post(() -> {
            updateAll();
            scheduleLatencyTest(700);
        });
    }

    private void scheduleLatencyTest(long delayMs) {
        mainHandler.removeCallbacks(latencyKick);
        mainHandler.postDelayed(latencyKick, delayMs);
    }

    private final Runnable latencyKick = this::runLatencyTest;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); g.setStroke(dp(1), line); return g;
    }
    private TextView tv(String s, int sp, int c, boolean bold) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); v.setPadding(0, dp(2), 0, dp(2));
        if (bold) v.setTypeface(null, 1); return v;
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(18),dp(18),dp(18)); c.setBackground(bg(panel,20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(12)); c.setLayoutParams(lp); return c;
    }
    private Button button(String s, boolean primary) {
        Button b = new Button(this); b.setText(s); b.setTextColor(text); b.setTextSize(16); b.setAllCaps(false); b.setTypeface(null,1); b.setBackground(bg(primary ? Color.rgb(75,116,220) : panel2,15));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(54)); lp.setMargins(0,dp(10),0,0); b.setLayoutParams(lp); return b;
    }

    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(7,10,18));
        getWindow().setNavigationBarColor(Color.rgb(7,10,18));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(20),dp(14),dp(30)); root.setBackgroundColor(Color.rgb(7,10,18));
        scroll.addView(root); setContentView(scroll);

        root.addView(tv("🎮  PS4 Unterwegs",26,text,true));
        root.addView(tv("Remote-Play-Begleiter · Performance-Modus",13,muted,false));
        Space sp = new Space(this); root.addView(sp,new LinearLayout.LayoutParams(1,dp(18)));

        LinearLayout c = card();
        statusTitle = tv("Prüfe Verbindung …",22,text,true);
        statusText = tv("Netzwerk und Controller werden bewertet.",14,muted,false);
        c.addView(statusTitle); c.addView(statusText);

        metrics = new LinearLayout(this); metrics.setOrientation(LinearLayout.HORIZONTAL); metrics.setPadding(0,dp(12),0,0);
        netType = metric(metrics,"Verbindung"); latency = metric(metrics,"Internet"); controller = metric(metrics,"Gamepad"); c.addView(metrics);

        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(dp(14),dp(14),dp(14),dp(14)); p.setBackground(bg(panel2,14));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1,-2); plp.setMargins(0,dp(12),0,0); p.setLayoutParams(plp);
        p.addView(tv("Empfohlenes Profil",12,muted,true)); profile = tv("Wird ermittelt …",20,text,true); p.addView(profile); c.addView(p);

        Button launch = button("Remote Play starten",true); launch.setOnClickListener(v -> launchRemotePlay()); c.addView(launch);
        Button test = button("Verbindung neu testen",false); test.setOnClickListener(v -> runLatencyTest()); c.addView(test);
        Button wireless = button("WLAN / Mobilfunk öffnen",false); wireless.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS))); c.addView(wireless);
        root.addView(c);

        LinearLayout checks = card();
        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ch = tv("PS4-Startcheck",18,text,true); head.addView(ch,new LinearLayout.LayoutParams(0,-2,1));
        checkCount = tv("0/5",13,muted,true); head.addView(checkCount); checks.addView(head); checklist = checks;
        addCheck("Remote Play auf der PS4 aktiviert","Remote-Play-Verbindungseinstellungen prüfen.",0);
        addCheck("PS4 für Ruhemodus vorbereitet","Internet aktiv lassen und Einschalten über Netzwerk erlauben.",1);
        addCheck("Spiel installiert und aktualisiert","Große Downloads besser zuhause erledigen.",2);
        addCheck("Controller gekoppelt","Bluetooth oder USB, je nach Controller.",3);
        addCheck("Heimnetz der PS4 stabil","LAN an der PS4 ist meist die sauberste Lösung.",4);
        root.addView(checks);

        LinearLayout data = card(); data.addView(tv("Datenverbrauch",18,text,true));
        dataText = tv("3,60 GB / Stunde",30,text,true); data.addView(dataText);
        TextView rateLabel = tv("angenommene Bitrate: 8 Mbit/s",13,muted,false); data.addView(rateLabel);
        SeekBar rate = new SeekBar(this); rate.setMax(17); rate.setProgress(prefs.getInt("bitrate",8)-3); data.addView(rate);
        rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int p,boolean f) {
                int mb = p + 3; prefs.edit().putInt("bitrate",mb).apply();
                rateLabel.setText("angenommene Bitrate: " + mb + " Mbit/s");
                dataText.setText(String.format(Locale.GERMANY,"%.2f GB / Stunde",mb*3600d/8d/1000d));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        int mb = prefs.getInt("bitrate",8); rateLabel.setText("angenommene Bitrate: " + mb + " Mbit/s");
        dataText.setText(String.format(Locale.GERMANY,"%.2f GB / Stunde",mb*3600d/8d/1000d));
        data.addView(tv("Schätzung. Tatsächlicher Verbrauch hängt von Szene, Codec und Remote-Play-Einstellung ab.",13,muted,false));
        root.addView(data);

        LinearLayout note = card();
        note.addView(tv("Die Profil-Empfehlung optimiert deine Entscheidung, nicht heimlich Sonys App. Einstellungen in PS Remote Play bleiben unter deiner Kontrolle.",14,Color.rgb(248,233,183),true));
        root.addView(note);
        root.addView(tv("Keine Sony-/PlayStation-App. Startet lediglich die separat installierte offizielle PS Remote Play-App.",12,muted,false));
    }

    private TextView metric(LinearLayout row, String label) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(9),dp(8),dp(9)); box.setBackground(bg(panel2,14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),0,dp(3),0); row.addView(box,lp);
        TextView val = tv("–",16,text,true); box.addView(val); box.addView(tv(label,11,muted,false)); return val;
    }

    private void addCheck(String title, String sub, int idx) {
        CheckBox cb = new CheckBox(this); cb.setText(title + "\n" + sub); cb.setTextColor(text); cb.setTextSize(14); cb.setPadding(0,dp(8),0,dp(8));
        cb.setChecked(prefs.getBoolean("check"+idx,false));
        cb.setOnCheckedChangeListener((b,c) -> { prefs.edit().putBoolean("check"+idx,c).apply(); updateCheckCount(); });
        checklist.addView(cb);
    }

    private void updateCheckCount() {
        int n = 0; for (int i=0;i<5;i++) if (prefs.getBoolean("check"+i,false)) n++;
        checkCount.setText(n + "/5");
    }

    private NetworkCapabilities activeCapabilities() {
        Network n = connectivityManager.getActiveNetwork();
        return n == null ? null : connectivityManager.getNetworkCapabilities(n);
    }

    private boolean hasValidatedInternet() {
        NetworkCapabilities c = activeCapabilities();
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private String transport() {
        NetworkCapabilities c = activeCapabilities();
        if (c == null) return "Offline";
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WLAN";
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobil";
        if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "LAN";
        return "Online";
    }

    private String gamepadName() {
        for (int id : android.view.InputDevice.getDeviceIds()) {
            android.view.InputDevice d = android.view.InputDevice.getDevice(id); if (d == null) continue;
            int s = d.getSources();
            if ((s & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD ||
                    (s & android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK) return d.getName();
        }
        return null;
    }

    private void updateController() {
        if (controller == null) return;
        controller.setText(gamepadName() == null ? "Nein" : "Ja"); updateStatus();
    }

    private void updateAll() {
        netType.setText(hasValidatedInternet() ? transport() : "Offline");
        updateController(); updateCheckCount(); updateStatus(); updateProfile();
    }

    private void updateProfile() {
        if (profile == null) return;
        if (!hasValidatedInternet()) { profile.setText("Offline · kein Stream"); profile.setTextColor(red); return; }
        String t = transport();
        if (lastLatency > 0 && lastLatency <= 45 && (t.equals("WLAN") || t.equals("LAN"))) {
            profile.setText("Qualität · stabile Verbindung bevorzugen"); profile.setTextColor(green);
        } else if (lastLatency > 0 && lastLatency <= 90) {
            profile.setText("Ausgewogen · gute Standardwahl"); profile.setTextColor(blue);
        } else if (lastLatency > 90) {
            profile.setText("Latenz · Qualität reduzieren"); profile.setTextColor(yellow);
        } else {
            profile.setText("Ausgewogen · Messung läuft"); profile.setTextColor(blue);
        }
    }

    private void updateStatus() {
        boolean online = hasValidatedInternet(); String pad = gamepadName();
        if (!online) {
            statusTitle.setText("Nicht spielbereit"); statusTitle.setTextColor(red); statusText.setText("Keine validierte Internetverbindung erkannt."); return;
        }
        if (lastLatency > 180) {
            statusTitle.setText("Verbindung kritisch"); statusTitle.setTextColor(yellow); statusText.setText("Hohe Internet-Reaktionszeit. Remote Play kann deutlich ruckeln."); return;
        }
        if (lastLatency > 0 && lastLatency <= 90) {
            statusTitle.setText("Bereit für Remote Play"); statusTitle.setTextColor(green);
            statusText.setText(pad == null ? "Netz sieht gut aus. Controller optional noch koppeln." : "Netz und Controller sehen gut aus."); return;
        }
        statusTitle.setText("Verbindung vorhanden"); statusTitle.setTextColor(yellow); statusText.setText("Netz ist da. Messung liefert die genauere Einschätzung.");
    }

    private void runLatencyTest() {
        if (!hasValidatedInternet()) { lastLatency = -1; latency.setText("n/a"); updateStatus(); updateProfile(); return; }
        if (!testRunning.compareAndSet(false,true)) return;
        latency.setText("…");
        new Thread(() -> {
            List<Long> samples = new ArrayList<>();
            for (int i=0;i<5;i++) {
                HttpURLConnection c = null;
                try {
                    long t = System.nanoTime();
                    c = (HttpURLConnection) new URL(TEST_URL).openConnection();
                    c.setRequestMethod("HEAD"); c.setConnectTimeout(3500); c.setReadTimeout(3500); c.setUseCaches(false);
                    c.setRequestProperty("Connection","close"); c.getResponseCode();
                    samples.add((System.nanoTime()-t)/1_000_000);
                } catch (Exception ignored) {
                } finally { if (c != null) c.disconnect(); }
            }
            Collections.sort(samples);
            lastLatency = samples.isEmpty() ? -1 : samples.get(samples.size()/2);
            testRunning.set(false);
            runOnUiThread(() -> {
                latency.setText(lastLatency < 0 ? "n/a" : lastLatency + " ms");
                updateAll();
            });
        }, "ps4u-latency").start();
    }

    private void launchRemotePlay() {
        PackageManager pm = getPackageManager(); Intent i = pm.getLaunchIntentForPackage(REMOTE_PACKAGE);
        if (i != null) { startActivity(i); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+REMOTE_PACKAGE))); }
        catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+REMOTE_PACKAGE))); }
    }
}
