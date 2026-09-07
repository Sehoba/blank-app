package de.moebelschroeder.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
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
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final String PREFS = "display_settings";

    private static final String[] BACKGROUNDS = {
            "#fffdf7", "#ffffff", "#f2f2f2", "#fff3b0"
    };
    private static final String[] BACKGROUND_NAMES = {
            "Warm", "Weiß", "Hellgrau", "Gelb"
    };

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private LinearLayout displayControls;
    private ValueCallback<Uri[]> fileCallback;
    private SharedPreferences prefs;
    private int fontZoom;
    private int backgroundIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        fontZoom = prefs.getInt("fontZoom", 108);
        backgroundIndex = prefs.getInt("backgroundIndex", 0);

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

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFDF7);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFFFFDF7);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollbarFadingEnabled(true);
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
        errorView.setBackgroundColor(0xFFFFFDF7);
        errorView.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("Keine Verbindung");
        title.setTextSize(22);
        title.setTextColor(0xFF222222);
        title.setGravity(Gravity.CENTER);
        errorView.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Die Möbel-Schröder-Webseite konnte gerade nicht geladen werden.");
        hint.setTextSize(15);
        hint.setTextColor(0xFF666666);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.setMargins(0, dp(12), 0, dp(20));
        errorView.addView(hint, hintParams);

        Button retry = new Button(this);
        retry.setText("Erneut versuchen");
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

        displayControls = new LinearLayout(this);
        displayControls.setOrientation(LinearLayout.HORIZONTAL);
        displayControls.setGravity(Gravity.CENTER);
        displayControls.setPadding(dp(5), dp(5), dp(5), dp(5));
        displayControls.setBackground(roundedBackground(0xEE222222, 22));
        displayControls.setElevation(dp(10));
        displayControls.setVisibility(View.GONE);

        displayControls.addView(makeControlButton("−", v -> pageZoom(0.88f)));
        displayControls.addView(makeControlButton("+", v -> pageZoom(1.14f)));
        displayControls.addView(makeControlButton("A−", v -> changeFont(-8)));
        displayControls.addView(makeControlButton("A+", v -> changeFont(8)));
        displayControls.addView(makeControlButton("◐", v -> cycleBackground()));

        FrameLayout.LayoutParams controlsParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        controlsParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        controlsParams.bottomMargin = dp(76);
        root.addView(displayControls, controlsParams);

        Button displayButton = new Button(this);
        displayButton.setText("Aa");
        displayButton.setTextSize(15);
        displayButton.setTextColor(Color.BLACK);
        displayButton.setAllCaps(false);
        displayButton.setPadding(0, 0, 0, 0);
        displayButton.setMinWidth(0);
        displayButton.setMinHeight(0);
        displayButton.setBackground(roundedBackground(0xFFFFD400, 28));
        displayButton.setElevation(dp(12));
        displayButton.setOnClickListener(v -> {
            displayControls.setVisibility(
                    displayControls.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
            );
            enterImmersiveMode();
        });

        FrameLayout.LayoutParams displayButtonParams = new FrameLayout.LayoutParams(dp(54), dp(54));
        displayButtonParams.gravity = Gravity.BOTTOM | Gravity.END;
        displayButtonParams.setMargins(dp(12), dp(12), dp(14), dp(14));
        root.addView(displayButton, displayButtonParams);

        setContentView(root);
    }

    private Button makeControlButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.BLACK);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(9), 0, dp(9), 0);
        button.setBackground(roundedBackground(0xFFFFD400, 16));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(42)
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable roundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void pageZoom(float factor) {
        try {
            webView.zoomBy(factor);
            Toast.makeText(this, factor > 1f ? "Ansicht vergrößert" : "Ansicht verkleinert", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
        enterImmersiveMode();
    }

    private void changeFont(int delta) {
        fontZoom = Math.max(80, Math.min(160, fontZoom + delta));
        webView.getSettings().setTextZoom(fontZoom);
        prefs.edit().putInt("fontZoom", fontZoom).apply();
        Toast.makeText(this, "Schrift: " + fontZoom + "%", Toast.LENGTH_SHORT).show();
        enterImmersiveMode();
    }

    private void cycleBackground() {
        backgroundIndex = (backgroundIndex + 1) % BACKGROUNDS.length;
        prefs.edit().putInt("backgroundIndex", backgroundIndex).apply();
        applyPageEnhancements();
        Toast.makeText(this, "Hintergrund: " + BACKGROUND_NAMES[backgroundIndex], Toast.LENGTH_SHORT).show();
        enterImmersiveMode();
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
        settings.setTextZoom(fontZoom);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);

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
                applyPageEnhancements();
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

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams
            ) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = filePathCallback;
                try {
                    startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Keine App zur Dateiauswahl gefunden.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
    }

    private void applyPageEnhancements() {
        String bg = BACKGROUNDS[backgroundIndex];
        String css =
                "html{-webkit-text-size-adjust:100%;width:100%!important;max-width:100%!important;overflow-x:hidden!important;}" +
                "html,body{box-sizing:border-box!important;}" +
                "body{width:100%!important;max-width:100%!important;margin:0 auto!important;padding:8px 10px 86px!important;overflow-x:hidden!important;background:" + bg + "!important;line-height:1.45!important;}" +
                "*,*:before,*:after{box-sizing:border-box!important;}" +
                "img{max-width:100%!important;height:auto!important;object-fit:contain!important;}" +
                "table{max-width:100%!important;}" +
                "iframe,video,object,embed{max-width:100%!important;height:auto!important;}" +
                "input,select,textarea,button{max-width:100%!important;}" +
                "p,li,td,th,a{overflow-wrap:anywhere;}" +
                "@media screen and (max-width:700px){" +
                "body{padding-left:8px!important;padding-right:8px!important;}" +
                "table[width]{width:100%!important;max-width:100%!important;}" +
                "td[width],div[style*='width']{max-width:100%!important;}" +
                "img[width]{max-width:100%!important;height:auto!important;}" +
                "}";

        String js = "(function(){" +
                "var h=document.head||document.getElementsByTagName('head')[0];" +
                "if(!h){return;}" +
                "var v=document.querySelector('meta[name=viewport]');" +
                "if(!v){v=document.createElement('meta');v.name='viewport';h.appendChild(v);}" +
                "v.content='width=device-width,initial-scale=1.0,maximum-scale=5.0,user-scalable=yes';" +
                "var s=document.getElementById('ms-extra-mobile-style');" +
                "if(!s){s=document.createElement('style');s.id='ms-extra-mobile-style';h.appendChild(s);}" +
                "s.textContent=" + JSONObject.quote(css) + ";" +
                "document.querySelectorAll('img').forEach(function(i){i.style.maxWidth='100%';i.style.height='auto';});" +
                "document.querySelectorAll('table[width]').forEach(function(t){t.style.maxWidth='100%';t.style.width='100%';});" +
                "})();";
        webView.evaluateJavascript(js, null);
        try {
            webView.setBackgroundColor(Color.parseColor(bg));
        } catch (Exception ignored) {
        }
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if ((scheme.equals("http") || scheme.equals("https")) && isInternalHost(host)) {
            return false;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ex) {
            Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private boolean isInternalHost(String host) {
        return host.equals("moebel-schroeder.net") || host.equals("www.moebel-schroeder.net");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
        getWindow().getDecorView().postDelayed(this::enterImmersiveMode, 150);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (displayControls != null && displayControls.getVisibility() == View.VISIBLE) {
            displayControls.setVisibility(View.GONE);
        } else if (webView != null && webView.canGoBack()) {
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
