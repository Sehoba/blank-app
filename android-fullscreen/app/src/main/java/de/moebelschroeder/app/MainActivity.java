package de.moebelschroeder.app3;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {
    private static final String BUNDLE_VERSION = "2026-09-07-v4";
    private static final String BUNDLE_NAME = "site_bundle.zip";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private LinearLayout submenu;
    private SharedPreferences prefs;
    private File siteDir;
    private boolean imageFit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("app3", MODE_PRIVATE);
        imageFit = prefs.getBoolean("imageFit", false);
        siteDir = new File(getFilesDir(), "site");

        prepareFullscreen();
        buildUi();
        configureWebView();
        enterImmersive();

        try {
            ensureSiteBundle();
            String localUrl = Uri.fromFile(new File(siteDir, "index.html")).toString();
            if (savedInstanceState == null) webView.loadUrl(localUrl); else webView.restoreState(savedInstanceState);
        } catch (Exception e) {
            showError("App-Inhalt konnte nicht vorbereitet werden.");
        }
    }

    private void ensureSiteBundle() throws Exception {
        String installed = prefs.getString("bundleVersion", "");
        File index = new File(siteDir, "index.html");
        if (BUNDLE_VERSION.equals(installed) && index.exists()) return;

        deleteRecursive(siteDir);
        if (!siteDir.mkdirs() && !siteDir.exists()) throw new IllegalStateException("site dir");

        try (InputStream raw = getAssets().open(BUNDLE_NAME);
             ZipInputStream zis = new ZipInputStream(raw)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(siteDir, entry.getName());
                String root = siteDir.getCanonicalPath() + File.separator;
                if (!out.getCanonicalPath().startsWith(root)) throw new SecurityException("Invalid zip path");
                if (entry.isDirectory()) {
                    if (!out.mkdirs() && !out.exists()) throw new IllegalStateException("mkdir");
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("parent");
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int read;
                        while ((read = zis.read(buffer)) > 0) fos.write(buffer, 0, read);
                    }
                }
                zis.closeEntry();
            }
        }
        prefs.edit().putString("bundleVersion", BUNDLE_VERSION).apply();
    }

    private void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        file.delete();
    }

    private void prepareFullscreen() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWindow().setDecorFitsSystemWindows(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams p = getWindow().getAttributes();
            p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(p);
        }
    }

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(255, 251, 234));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(255, 251, 234));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            progressBar.setProgressTintList(ColorStateList.valueOf(0xFFFFD400));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(-1, dp(3));
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorView.setBackgroundColor(0xFFFFFBEA);
        errorView.setVisibility(View.GONE);
        TextView errorText = new TextView(this);
        errorText.setText("Möbel Schröder\n\nDie Seite konnte gerade nicht geladen werden.");
        errorText.setTextColor(0xFF0B3C8C);
        errorText.setTextSize(20);
        errorText.setGravity(Gravity.CENTER);
        errorView.addView(errorText);
        Button retry = navButton("Neu laden", true);
        retry.setOnClickListener(v -> reloadPage());
        errorView.addView(retry);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout dockShell = new LinearLayout(this);
        dockShell.setOrientation(LinearLayout.VERTICAL);
        dockShell.setGravity(Gravity.CENTER);
        dockShell.setPadding(dp(7), dp(7), dp(7), dp(7));
        dockShell.setBackgroundColor(0xF20B3C8C);
        dockShell.setElevation(dp(12));

        submenu = new LinearLayout(this);
        submenu.setOrientation(LinearLayout.VERTICAL);
        submenu.setVisibility(View.GONE);
        submenu.setPadding(0, 0, 0, dp(6));

        LinearLayout subRow1 = row();
        addSubButton(subRow1, "Sortiment", "sortiment");
        addSubButton(subRow1, "Beratung", "beratung");
        addSubButton(subRow1, "Über uns", "unternehmen");
        submenu.addView(subRow1);

        LinearLayout subRow2 = row();
        addSubButton(subRow2, "Kontakt", "kontakt");
        Button image = navButton("Bilder", false);
        image.setOnClickListener(v -> toggleImageMode());
        subRow2.addView(image, weighted());
        Button reload = navButton("Neu laden", false);
        reload.setOnClickListener(v -> reloadPage());
        subRow2.addView(reload, weighted());
        submenu.addView(subRow2);
        dockShell.addView(submenu);

        LinearLayout primary = row();
        Button back = navButton("← Zurück", true);
        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); else scrollTo("top"); });
        primary.addView(back, weighted());

        Button home = navButton("⌂ Start", true);
        home.setOnClickListener(v -> scrollTo("top"));
        primary.addView(home, weighted());

        Button menu = navButton("☰ Menü", true);
        menu.setOnClickListener(v -> {
            submenu.setVisibility(submenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            enterImmersive();
        });
        primary.addView(menu, weighted());
        dockShell.addView(primary);

        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-1, -2);
        dockParams.gravity = Gravity.BOTTOM;
        dockParams.setMargins(dp(8), 0, dp(8), dp(8));
        root.addView(dockShell, dockParams);
        setContentView(root);
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(46), 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        return p;
    }

    private void addSubButton(LinearLayout row, String label, String section) {
        Button b = navButton(label, false);
        b.setOnClickListener(v -> { scrollTo(section); submenu.setVisibility(View.GONE); });
        row.addView(b, weighted());
    }

    private Button navButton(String text, boolean strong) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(strong ? 13 : 12);
        b.setTextColor(0xFF082F70);
        b.setPadding(dp(5), 0, dp(5), 0);
        b.setMinHeight(dp(44));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            b.setBackgroundTintList(ColorStateList.valueOf(strong ? 0xFFFFD400 : 0xFFFFF2A8));
        return b;
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            s.setAllowFileAccessFromFileURLs(false);
            s.setAllowUniversalAccessFromFileURLs(false);
        }

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                progressBar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorView.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                applyImageMode();
                enterImmersive();
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError("Die Seite konnte gerade nicht geladen werden.");
            }
        });
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("file") || scheme.equals("about")) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void toggleImageMode() {
        imageFit = !imageFit;
        prefs.edit().putBoolean("imageFit", imageFit).apply();
        applyImageMode();
        Toast.makeText(this, imageFit ? "Bilder vollständig eingepasst" : "Bilder wieder großflächig", Toast.LENGTH_SHORT).show();
    }

    private void applyImageMode() {
        String js = "document.documentElement.classList." + (imageFit ? "add" : "remove") + "('app-image-fit');";
        webView.evaluateJavascript(js, null);
    }

    private void scrollTo(String id) {
        String safe = id.replace("'", "");
        webView.evaluateJavascript("(function(){var e=document.getElementById('" + safe + "');if(e)e.scrollIntoView({behavior:'smooth',block:'start'});})();", null);
        enterImmersive();
    }

    private void reloadPage() {
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.reload();
        enterImmersive();
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() { super.onResume(); getWindow().getDecorView().post(this::enterImmersive); }
    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) getWindow().getDecorView().postDelayed(this::enterImmersive, 100); }
    @Override protected void onSaveInstanceState(Bundle out) { if (webView != null) webView.saveState(out); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() { if (submenu != null && submenu.getVisibility() == View.VISIBLE) { submenu.setVisibility(View.GONE); return; } if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
