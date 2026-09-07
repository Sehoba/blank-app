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

import org.json.JSONObject;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://moebel-schroeder.net/index.html/";
    private static final int[] TEXT_ZOOMS = {100, 115, 130};
    private static final String[] BG = {"#EFE7DA", "#FFFFFF", "#171717"};
    private static final String[] FG = {"#24211D", "#222222", "#F4F0E8"};
    private static final String[] CARD = {"#FFFDF8", "#F6F6F6", "#242424"};
    private static final String[] LINK = {"#7A5A00", "#705000", "#FFD400"};

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout dock;
    private LinearLayout errorView;
    private SharedPreferences prefs;
    private int themeIndex;
    private int textIndex;
    private boolean imageFullWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("showroom", MODE_PRIVATE);
        themeIndex = Math.max(0, Math.min(2, prefs.getInt("theme", 0)));
        textIndex = Math.max(0, Math.min(2, prefs.getInt("text", 1)));
        imageFullWidth = prefs.getBoolean("imageFullWidth", true);
        prepareFullscreen();
        buildUi();
        configureWebView();
        enterImmersive();
        if (savedInstanceState == null) webView.loadUrl(HOME_URL); else webView.restoreState(savedInstanceState);
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
        root.setBackgroundColor(Color.parseColor(BG[themeIndex]));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor(BG[themeIndex]));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            progressBar.setProgressTintList(ColorStateList.valueOf(0xFFFFD400));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, dp(3));
        pp.gravity = Gravity.TOP;
        root.addView(progressBar, pp);

        TextView badge = new TextView(this);
        badge.setText("SHOWROOM #3");
        badge.setTextColor(0xFF202020);
        badge.setTextSize(12);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(0xEFFFD400);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(118), dp(32));
        bp.gravity = Gravity.TOP | Gravity.END;
        bp.setMargins(0, dp(10), dp(10), 0);
        root.addView(badge, bp);

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorView.setBackgroundColor(Color.parseColor(BG[themeIndex]));
        errorView.setVisibility(View.GONE);
        TextView error = new TextView(this);
        error.setText("Showroom offline\n\nInternetverbindung prüfen.");
        error.setTextSize(20);
        error.setGravity(Gravity.CENTER);
        error.setTextColor(Color.parseColor(FG[themeIndex]));
        errorView.addView(error);
        Button retry = button("Neu laden");
        retry.setOnClickListener(v -> { errorView.setVisibility(View.GONE); webView.setVisibility(View.VISIBLE); webView.reload(); });
        errorView.addView(retry);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));

        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(5), dp(5), dp(5), dp(5));
        dock.setBackgroundColor(0xEE202020);

        Button back = button("←");
        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        dock.addView(back);
        Button home = button("⌂");
        home.setOnClickListener(v -> webView.loadUrl(HOME_URL));
        dock.addView(home);
        Button fit = button("Bild");
        fit.setOnClickListener(v -> { imageFullWidth = !imageFullWidth; prefs.edit().putBoolean("imageFullWidth", imageFullWidth).apply(); applyStyle(); });
        dock.addView(fit);
        Button text = button("Aa");
        text.setOnClickListener(v -> { textIndex = (textIndex + 1) % TEXT_ZOOMS.length; prefs.edit().putInt("text", textIndex).apply(); webView.getSettings().setTextZoom(TEXT_ZOOMS[textIndex]); });
        dock.addView(text);
        Button theme = button("◐");
        theme.setOnClickListener(v -> { themeIndex = (themeIndex + 1) % BG.length; prefs.edit().putInt("theme", themeIndex).apply(); webView.setBackgroundColor(Color.parseColor(BG[themeIndex])); errorView.setBackgroundColor(Color.parseColor(BG[themeIndex])); applyStyle(); });
        dock.addView(theme);

        FrameLayout.LayoutParams dpDock = new FrameLayout.LayoutParams(-2, dp(56));
        dpDock.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        dpDock.setMargins(dp(8), 0, dp(8), dp(14));
        root.addView(dock, dpDock);
        setContentView(root);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(0xFF202020);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setMinWidth(dp(54));
        b.setMinHeight(dp(44));
        b.setPadding(dp(10), 0, dp(10), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            b.setBackgroundTintList(ColorStateList.valueOf(0xFFFFD400));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(44));
        p.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(p);
        return b;
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setTextZoom(TEXT_ZOOMS[textIndex]);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);

        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int p) {
                progressBar.setProgress(p);
                progressBar.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return handle(req.getUrl()); }
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) { errorView.setVisibility(View.GONE); view.setVisibility(View.VISIBLE); progressBar.setVisibility(View.VISIBLE); }
            @Override public void onPageFinished(WebView view, String url) { progressBar.setVisibility(View.GONE); applyStyle(); enterImmersive(); }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) { if (req.isForMainFrame()) { progressBar.setVisibility(View.GONE); view.setVisibility(View.GONE); errorView.setVisibility(View.VISIBLE); } }
        });
        webView.setOnScrollChangeListener((v,x,y,ox,oy) -> {
            if (y > oy + dp(6)) dock.animate().translationY(dp(86)).alpha(0.15f).setDuration(160).start();
            else if (y < oy - dp(6)) dock.animate().translationY(0).alpha(1f).setDuration(140).start();
        });
    }

    private boolean handle(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if ((scheme.equals("http") || scheme.equals("https")) && (host.equals("moebel-schroeder.net") || host.equals("www.moebel-schroeder.net"))) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception e) { Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    private void applyStyle() {
        String img = imageFullWidth ? "img,picture img{width:100% !important;max-width:100% !important;height:auto !important;max-height:58vh !important;object-fit:cover !important;border-radius:16px !important;}" : "img,picture img{max-width:100% !important;width:auto !important;height:auto !important;max-height:58vh !important;object-fit:contain !important;border-radius:12px !important;}";
        String css = "html{background:"+BG[themeIndex]+" !important;-webkit-text-size-adjust:100% !important;}body{background:"+BG[themeIndex]+" !important;color:"+FG[themeIndex]+" !important;max-width:100vw !important;overflow-x:hidden !important;margin:0 !important;padding:12px 12px 90px !important;box-sizing:border-box !important;font-family:Arial,Helvetica,sans-serif !important;line-height:1.58 !important;}body>*{box-sizing:border-box !important;max-width:100% !important;}#wrapper,#page,#content,.wrapper,.page,.content,.container,main,article,section{max-width:100% !important;box-sizing:border-box !important;}main,article,.content,#content{background:"+CARD[themeIndex]+" !important;border-radius:18px !important;padding:clamp(10px,3vw,22px) !important;}"+img+"svg,video,iframe{max-width:100% !important;height:auto !important;}h1,h2,h3,h4,p,li,td,th,span,label{color:"+FG[themeIndex]+" !important;}h1{font-size:clamp(28px,7.7vw,44px) !important;line-height:1.08 !important;}h2{font-size:clamp(23px,6.2vw,34px) !important;}h3{font-size:clamp(20px,5vw,28px) !important;}a{color:"+LINK[themeIndex]+" !important;font-weight:600 !important;}table{display:block !important;max-width:100% !important;overflow-x:auto !important;}input,textarea,select,button{max-width:100% !important;box-sizing:border-box !important;min-height:44px !important;}";
        String js = "(function(){var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','width=device-width,initial-scale=1.0,minimum-scale=0.6,maximum-scale=4.0,user-scalable=yes');var o=document.getElementById('ms-showroom-style');if(o)o.remove();var st=document.createElement('style');st.id='ms-showroom-style';st.textContent="+JSONObject.quote(css)+";document.head.appendChild(st);})();";
        webView.evaluateJavascript(js, null);
    }

    @Override protected void onResume() { super.onResume(); getWindow().getDecorView().post(this::enterImmersive); }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) getWindow().getDecorView().postDelayed(this::enterImmersive, 120); }
    @Override protected void onSaveInstanceState(Bundle out) { webView.saveState(out); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
