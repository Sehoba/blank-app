package de.moebelschroeder.app2;

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

import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://moebel-schroeder.net/index.html/";
    private static final String[] BACKGROUNDS = {"#FFF7D6", "#FFFFFF", "#F1ECE3"};

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private LinearLayout controlsPanel;
    private int textZoom = 110;
    private int backgroundIndex = 0;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("display", MODE_PRIVATE);
        textZoom = preferences.getInt("textZoom", 110);
        backgroundIndex = preferences.getInt("backgroundIndex", 0);
        if (backgroundIndex < 0 || backgroundIndex >= BACKGROUNDS.length) backgroundIndex = 0;

        prepareFullscreenWindow();
        buildUi();
        configureWebView();
        enterImmersiveMode();

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void prepareFullscreenWindow() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor(BACKGROUNDS[backgroundIndex]));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor(BACKGROUNDS[backgroundIndex]));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(3)
        );
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorView.setBackgroundColor(Color.parseColor(BACKGROUNDS[backgroundIndex]));
        errorView.setVisibility(View.GONE);

        TextView errorTitle = new TextView(this);
        errorTitle.setText("Keine Verbindung");
        errorTitle.setTextSize(23);
        errorTitle.setTextColor(0xFF202020);
        errorTitle.setGravity(Gravity.CENTER);
        errorView.addView(errorTitle);

        TextView errorHint = new TextView(this);
        errorHint.setText("Die Möbel-Schröder-Webseite konnte gerade nicht geladen werden.");
        errorHint.setTextSize(16);
        errorHint.setTextColor(0xFF555555);
        errorHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(12), 0, dp(18));
        errorView.addView(errorHint, hintParams);

        Button retry = makeControlButton("Neu laden");
        retry.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.reload();
            enterImmersiveMode();
        });
        errorView.addView(retry);
        root.addView(errorView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout controlShell = new LinearLayout(this);
        controlShell.setOrientation(LinearLayout.VERTICAL);
        controlShell.setGravity(Gravity.END);

        controlsPanel = new LinearLayout(this);
        controlsPanel.setOrientation(LinearLayout.HORIZONTAL);
        controlsPanel.setGravity(Gravity.CENTER_VERTICAL);
        controlsPanel.setPadding(dp(4), dp(4), dp(4), dp(4));
        controlsPanel.setVisibility(View.GONE);

        Button textMinus = makeControlButton("A−");
        textMinus.setOnClickListener(v -> changeTextZoom(-10));
        controlsPanel.addView(textMinus);

        Button textPlus = makeControlButton("A+");
        textPlus.setOnClickListener(v -> changeTextZoom(10));
        controlsPanel.addView(textPlus);

        Button zoomOut = makeControlButton("−");
        zoomOut.setOnClickListener(v -> webView.zoomOut());
        controlsPanel.addView(zoomOut);

        Button zoomIn = makeControlButton("+");
        zoomIn.setOnClickListener(v -> webView.zoomIn());
        controlsPanel.addView(zoomIn);

        Button background = makeControlButton("BG");
        background.setOnClickListener(v -> cycleBackground());
        controlsPanel.addView(background);

        controlShell.addView(controlsPanel);

        Button controlsToggle = makeControlButton("Aa");
        controlsToggle.setOnClickListener(v ->
                controlsPanel.setVisibility(controlsPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        LinearLayout.LayoutParams toggleParams = new LinearLayout.LayoutParams(dp(58), dp(48));
        toggleParams.gravity = Gravity.END;
        toggleParams.setMargins(0, dp(5), 0, 0);
        controlShell.addView(controlsToggle, toggleParams);

        FrameLayout.LayoutParams shellParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        shellParams.gravity = Gravity.END | Gravity.BOTTOM;
        shellParams.setMargins(dp(8), dp(8), dp(10), dp(16));
        root.addView(controlShell, shellParams);

        setContentView(root);
    }

    private Button makeControlButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xFF161616);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setMinWidth(dp(48));
        button.setMinHeight(dp(44));
        button.setPadding(dp(8), 0, dp(8), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setBackgroundTintList(ColorStateList.valueOf(0xEEFFD400));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        );
        params.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(textZoom);
        settings.setDefaultFontSize(16);
        settings.setDefaultFixedFontSize(14);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

        webView.setInitialScale(100);

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorView.setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                applyMobileStyle();
                enterImmersiveMode();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    errorView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if ((scheme.equals("http") || scheme.equals("https"))
                && (host.equals("moebel-schroeder.net") || host.equals("www.moebel-schroeder.net"))) {
            return false;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ex) {
            Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void changeTextZoom(int delta) {
        textZoom = Math.max(80, Math.min(160, textZoom + delta));
        webView.getSettings().setTextZoom(textZoom);
        preferences.edit().putInt("textZoom", textZoom).apply();
        Toast.makeText(this, "Schrift: " + textZoom + "%", Toast.LENGTH_SHORT).show();
    }

    private void cycleBackground() {
        backgroundIndex = (backgroundIndex + 1) % BACKGROUNDS.length;
        preferences.edit().putInt("backgroundIndex", backgroundIndex).apply();
        int color = Color.parseColor(BACKGROUNDS[backgroundIndex]);
        webView.setBackgroundColor(color);
        errorView.setBackgroundColor(color);
        applyMobileStyle();
    }

    private void applyMobileStyle() {
        String background = BACKGROUNDS[backgroundIndex];
        String css =
                "html{background:" + background + " !important;-webkit-text-size-adjust:100% !important;}" +
                "body{background:" + background + " !important;color:#242424 !important;max-width:100vw !important;" +
                "overflow-x:hidden !important;margin:0 auto !important;padding:clamp(8px,2.5vw,18px) !important;" +
                "box-sizing:border-box !important;font-family:Arial,Helvetica,sans-serif !important;line-height:1.55 !important;}" +
                "body>*{box-sizing:border-box !important;}" +
                "#wrapper,#page,#content,.wrapper,.page,.content,.container,main,section,article{max-width:100% !important;box-sizing:border-box !important;}" +
                "img,picture,svg{max-width:100% !important;height:auto !important;object-fit:contain !important;}" +
                "video,iframe{max-width:100% !important;width:100% !important;height:auto !important;}" +
                "table{max-width:100% !important;width:100% !important;border-collapse:collapse !important;}" +
                "pre,code{max-width:100% !important;overflow-x:auto !important;}" +
                "p,li,td,th,label,input,textarea,select,button{line-height:1.55 !important;overflow-wrap:anywhere !important;}" +
                "h1{font-size:clamp(27px,7.5vw,42px) !important;line-height:1.12 !important;}" +
                "h2{font-size:clamp(23px,6.2vw,34px) !important;line-height:1.18 !important;}" +
                "h3{font-size:clamp(20px,5.2vw,28px) !important;line-height:1.22 !important;}" +
                "a{overflow-wrap:anywhere !important;}" +
                "input,textarea,select,button{max-width:100% !important;box-sizing:border-box !important;min-height:42px !important;}" +
                "@media(max-width:720px){" +
                "body{width:100% !important;padding:10px !important;}" +
                "img{max-height:70vh !important;}" +
                "table{display:block !important;overflow-x:auto !important;-webkit-overflow-scrolling:touch !important;}" +
                "}";

        String js = "(function(){" +
                "var m=document.querySelector('meta[name=viewport]');" +
                "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}" +
                "m.setAttribute('content','width=device-width, initial-scale=1.0, minimum-scale=0.5, maximum-scale=5.0, user-scalable=yes');" +
                "var old=document.getElementById('ms-mobile-app-style');if(old){old.remove();}" +
                "var s=document.createElement('style');s.id='ms-mobile-app-style';s.textContent=" + JSONObject.quote(css) + ";document.head.appendChild(s);" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().post(this::enterImmersiveMode);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().postDelayed(this::enterImmersiveMode, 120);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
