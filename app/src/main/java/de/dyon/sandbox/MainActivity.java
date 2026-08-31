package de.dyon.sandbox;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity implements ScreenCaptureService.FrameListener {
    private static final int REQUEST_CAPTURE = 4400;
    private static final int REQUEST_NOTIFICATIONS = 4401;
    private WebView webView;
    private MediaProjectionManager projectionManager;
    private SharedPreferences prefs;
    private boolean usbConnected = false;
    private boolean usbConfigured = false;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if ("android.hardware.usb.action.USB_STATE".equals(intent.getAction())) {
                usbConnected = intent.getBooleanExtra("connected", false);
                usbConfigured = intent.getBooleanExtra("configured", false);
                emitUsbState();
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("dyon", MODE_PRIVATE);
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new NativeBridge(), "DyonNative");
        webView.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        ScreenCaptureService.setFrameListener(this);
        IntentFilter filter = new IntentFilter("android.hardware.usb.action.USB_STATE");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(usbReceiver, filter);
        emitUsbState();
    }

    @Override protected void onStop() {
        ScreenCaptureService.setFrameListener(null);
        try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent service = new Intent(this, ScreenCaptureService.class);
                service.setAction(ScreenCaptureService.ACTION_START);
                service.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
                service.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data);
                service.putExtra(ScreenCaptureService.EXTRA_BRIDGE_URL, prefs.getString("bridgeUrl", ""));
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
                js("window.DYON && window.DYON.onCaptureState(true, 'Startet…')");
            } else {
                js("window.DYON && window.DYON.onCaptureState(false, 'Bildschirmfreigabe abgelehnt')");
            }
        }
    }

    private void requestCapture() {
        try {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Screen Capture nicht verfügbar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopCapture() {
        Intent service = new Intent(this, ScreenCaptureService.class);
        service.setAction(ScreenCaptureService.ACTION_STOP);
        startService(service);
    }

    private void emitUsbState() {
        UsbManager manager = (UsbManager) getSystemService(USB_SERVICE);
        int hostDevices = manager == null ? 0 : manager.getDeviceList().size();
        String script = "window.DYON && window.DYON.onUsbState(" + usbConnected + "," + usbConfigured + "," + hostDevices + ")";
        js(script);
    }

    private void js(String script) {
        if (webView == null) return;
        runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }

    @Override public void onFrame(String dataUrl, int fps, int kbps) {
        String safe = dataUrl.replace("\\", "\\\\").replace("'", "\\'");
        js("window.DYON && window.DYON.onFrame('" + safe + "'," + fps + "," + kbps + ")");
    }

    @Override public void onState(boolean running, String message) {
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("'", "\\'");
        js("window.DYON && window.DYON.onCaptureState(" + running + ",'" + safe + "')");
    }

    @Override public void onBridgeState(boolean connected, String message) {
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("'", "\\'");
        js("window.DYON && window.DYON.onBridgeState(" + connected + ",'" + safe + "')");
    }

    public class NativeBridge {
        @JavascriptInterface public void startScreenMirror() { runOnUiThread(MainActivity.this::requestCapture); }
        @JavascriptInterface public void stopScreenMirror() { runOnUiThread(MainActivity.this::stopCapture); }
        @JavascriptInterface public String getBridgeUrl() { return prefs.getString("bridgeUrl", ""); }
        @JavascriptInterface public void setBridgeUrl(String url) {
            String clean = url == null ? "" : url.trim().replaceAll("/+$", "");
            prefs.edit().putString("bridgeUrl", clean).apply();
            ScreenCaptureService.setBridgeUrl(clean);
        }
        @JavascriptInterface public String getCapabilities() {
            return "{\"screenCapture\":true,\"sandbox\":true,\"nativeUsbMassStorageGadget\":false,\"bridgeStreaming\":true}";
        }
        @JavascriptInterface public void refreshUsb() { runOnUiThread(MainActivity.this::emitUsbState); }
    }
}
