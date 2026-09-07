package de.moebelschroeder.app3;

import android.app.Activity;
import android.content.Intent;
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
    private static final int BLUE = 0xFF0B3D91;
    private static final int BLUE_DARK = 0xFF062A67;
    private static final int YELLOW = 0xFFFFD400;
    private static final int YELLOW_SOFT = 0xFFFFF1A8;
    private static final int PAGE = 0xFFF6F8FC;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout mainDock;
    private LinearLayout subDock;
    private LinearLayout errorView;
    private boolean subMenuOpen = false;
    private boolean imageCover = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        root.setBackgroundColor(PAGE);

        webView = new WebView(this);
        webView.setBackgroundColor(PAGE);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            progressBar.setProgressTintList(ColorStateList.valueOf(YELLOW));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, dp(4));
        pp.gravity = Gravity.TOP;
        root.addView(progressBar, pp);

        TextView badge = new TextView(this);
        badge.setText("MÖBEL SCHRÖDER  #3");
        badge.setTextColor(BLUE_DARK);
        badge.setTextSize(12);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundColor(YELLOW);
        badge.setPadding(dp(10), 0, dp(10), 0);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(154), dp(34));
        bp.gravity = Gravity.TOP | Gravity.END;
        bp.setMargins(0, dp(10), dp(10), 0);
        root.addView(badge, bp);

        errorView = new LinearLayout(this);
        errorView.setOrientation(LinearLayout.VERTICAL);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorView.setBackgroundColor(PAGE);
        errorView.setVisibility(View.GONE);
        TextView error = new TextView(this);
        error.setText("Keine Verbindung\n\nDie Möbel-Schröder-Seite konnte nicht geladen werden.");
        error.setTextSize(19);
        error.setTextColor(BLUE_DARK);
        error.setGravity(Gravity.CENTER);
        errorView.addView(error);
        Button retry = dockButton("Neu laden");
        retry.setOnClickListener(v -> { errorView.setVisibility(View.GONE); webView.setVisibility(View.VISIBLE); webView.reload(); });
        errorView.addView(retry);
        root.addView(errorView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout dockShell = new LinearLayout(this);
        dockShell.setOrientation(LinearLayout.VERTICAL);
        dockShell.setGravity(Gravity.CENTER_HORIZONTAL);

        subDock = new LinearLayout(this);
        subDock.setOrientation(LinearLayout.HORIZONTAL);
        subDock.setGravity(Gravity.CENTER);
        subDock.setPadding(dp(6), dp(5), dp(6), dp(5));
        subDock.setBackgroundColor(BLUE_DARK);
        subDock.setVisibility(View.GONE);

        Button top = dockButton("↑ Oben");
        top.setOnClickListener(v -> webView.scrollTo(0, 0));
        subDock.addView(top);

        Button reload = dockButton("↻ Neu");
        reload.setOnClickListener(v -> webView.reload());
        subDock.addView(reload);

        Button image = dockButton("▣ Bild");
        image.setOnClickListener(v -> {
            imageCover = !imageCover;
            applyStyle();
            Toast.makeText(this, imageCover ? "Bilder: großflächig" : "Bilder: vollständig sichtbar", Toast.LENGTH_SHORT).show();
        });
        subDock.addView(image);

        dockShell.addView(subDock, new LinearLayout.LayoutParams(-2, dp(54)));

        mainDock = new LinearLayout(this);
        mainDock.setOrientation(LinearLayout.HORIZONTAL);
        mainDock.setGravity(Gravity.CENTER);
        mainDock.setPadding(dp(6), dp(5), dp(6), dp(5));
        mainDock.setBackgroundColor(BLUE);

        Button back = dockButton("← Zurück");
        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        mainDock.addView(back);

        Button home = dockButton("⌂ Start");
        home.setOnClickListener(v -> webView.loadUrl(HOME_URL));
        mainDock.addView(home);

        Button menu = dockButton("☰ Menü");
        menu.setOnClickListener(v -> {
            subMenuOpen = !subMenuOpen;
            subDock.setVisibility(subMenuOpen ? View.VISIBLE : View.GONE);
        });
        mainDock.addView(menu);

        dockShell.addView(mainDock, new LinearLayout.LayoutParams(-2, dp(58)));

        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-2, -2);
        dockParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        dockParams.setMargins(dp(8), 0, dp(8), dp(12));
        root.addView(dockShell, dockParams);

        setContentView(root);
    }

    private Button dockButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(BLUE_DARK);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setMinWidth(dp(72));
        b.setMinHeight(dp(44));
        b.setPadding(dp(10), 0, dp(10), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            b.setBackgroundTintList(ColorStateList.valueOf(YELLOW));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(44));
        p.setMargins(dp(3), 0, dp(3), 0);
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
        s.setTextZoom(100);
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
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorView.setVisibility(View.GONE); view.setVisibility(View.VISIBLE); progressBar.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE); applyStyle(); enterImmersive();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) { progressBar.setVisibility(View.GONE); view.setVisibility(View.GONE); errorView.setVisibility(View.VISIBLE); }
            }
        });

        webView.setOnScrollChangeListener((v,x,y,ox,oy) -> {
            if (y > oy + dp(10) && !subMenuOpen) mainDock.animate().translationY(dp(72)).alpha(0.25f).setDuration(150).start();
            else if (y < oy - dp(10)) mainDock.animate().translationY(0).alpha(1f).setDuration(140).start();
        });
    }

    private boolean handle(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if ((scheme.equals("http") || scheme.equals("https")) && (host.equals("moebel-schroeder.net") || host.equals("www.moebel-schroeder.net"))) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (Exception e) { Toast.makeText(this, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show(); }
        return true;
    }

    private void applyStyle() {
        String imageCss = imageCover
                ? "img,picture img{width:100% !important;height:clamp(190px,46vw,320px) !important;object-fit:cover !important;border-radius:16px !important;}"
                : "img,picture img{max-width:100% !important;width:auto !important;height:auto !important;max-height:62vh !important;object-fit:contain !important;border-radius:14px !important;}";

        String css =
                "html{background:#F6F8FC !important;-webkit-text-size-adjust:100% !important;}"+
                "body{background:#F6F8FC !important;color:#0B2E63 !important;max-width:100vw !important;overflow-x:hidden !important;margin:0 !important;padding:10px 10px 104px !important;box-sizing:border-box !important;font-family:Arial,Helvetica,sans-serif !important;line-height:1.5 !important;}"+
                "body>*{box-sizing:border-box !important;max-width:100% !important;}"+
                "#wrapper,#page,#content,.wrapper,.page,.content,.container,main,article,section,header,footer,nav{max-width:100% !important;box-sizing:border-box !important;}"+
                "header,.header,#header,nav,.nav,.menu,.navigation{background:#0B3D91 !important;color:#FFFFFF !important;border-radius:14px !important;padding:10px !important;margin-bottom:10px !important;}"+
                "main,article,.content,#content,section{background:#FFFFFF !important;border:2px solid #DCE6F8 !important;border-radius:18px !important;padding:clamp(12px,3.4vw,22px) !important;margin:10px 0 !important;box-shadow:0 5px 16px rgba(11,61,145,.09) !important;}"+
                "section:nth-of-type(odd),article:nth-of-type(odd){border-top:5px solid #FFD400 !important;}"+
                imageCss+
                "svg,video,iframe{max-width:100% !important;height:auto !important;}"+
                "h1,h2,h3,h4{color:#0B3D91 !important;margin-top:.55em !important;margin-bottom:.45em !important;}"+
                "h1{font-size:clamp(28px,7.4vw,42px) !important;line-height:1.08 !important;}"+
                "h2{font-size:clamp(23px,6vw,32px) !important;line-height:1.15 !important;}"+
                "h3{font-size:clamp(19px,5vw,26px) !important;line-height:1.2 !important;}"+
                "p,li,td,th,label,span{color:#173A6A !important;overflow-wrap:anywhere !important;}"+
                "a{color:#0B3D91 !important;font-weight:700 !important;text-decoration-color:#FFD400 !important;text-decoration-thickness:2px !important;}"+
                "button,input[type=button],input[type=submit],.button,.btn{background:#FFD400 !important;color:#062A67 !important;border:0 !important;border-radius:12px !important;font-weight:700 !important;min-height:44px !important;padding:10px 14px !important;}"+
                "input,textarea,select{max-width:100% !important;width:100% !important;box-sizing:border-box !important;border:1px solid #AFC4E8 !important;border-radius:10px !important;padding:10px !important;background:#FFFFFF !important;color:#0B2E63 !important;}"+
                "table{display:block !important;width:100% !important;max-width:100% !important;overflow-x:auto !important;-webkit-overflow-scrolling:touch !important;border-radius:12px !important;}"+
                "ul,ol{padding-left:22px !important;}"+
                "@media(max-width:720px){header,.header,#header,nav,.nav,.menu,.navigation{display:flex !important;flex-wrap:wrap !important;gap:8px !important;align-items:center !important;}section,article,.content,#content{width:100% !important;margin:8px 0 !important;}img{margin:8px auto !important;display:block !important;}}";

        String js = "(function(){"+
                "var m=document.querySelector('meta[name=viewport]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.setAttribute('content','width=device-width,initial-scale=1.0,minimum-scale=0.75,maximum-scale=4.0,user-scalable=yes');"+
                "var o=document.getElementById('ms-blue-yellow-style');if(o)o.remove();var st=document.createElement('style');st.id='ms-blue-yellow-style';st.textContent="+JSONObject.quote(css)+";document.head.appendChild(st);"+
                "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override protected void onResume() { super.onResume(); getWindow().getDecorView().post(this::enterImmersive); }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) getWindow().getDecorView().postDelayed(this::enterImmersive, 120); }
    @Override protected void onSaveInstanceState(Bundle out) { webView.saveState(out); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() { if (subMenuOpen) { subMenuOpen=false; subDock.setVisibility(View.GONE); } else if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
