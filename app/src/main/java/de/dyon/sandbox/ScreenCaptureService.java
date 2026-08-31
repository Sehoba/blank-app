package de.dyon.sandbox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {
    public static final String ACTION_START = "de.dyon.sandbox.START_CAPTURE";
    public static final String ACTION_STOP = "de.dyon.sandbox.STOP_CAPTURE";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";
    public static final String EXTRA_BRIDGE_URL = "bridgeUrl";

    private static final int WIDTH = 480;
    private static final int HEIGHT = 234;
    private static final int FPS_LIMIT = 6;
    private static final int JPEG_QUALITY = 32;
    private static final int NOTIFICATION_ID = 234;
    private static WeakReference<FrameListener> listenerRef = new WeakReference<>(null);
    private static volatile String bridgeUrl = "";

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private final ExecutorService frameExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean frameBusy = new AtomicBoolean(false);
    private final AtomicBoolean uploadBusy = new AtomicBoolean(false);
    private long lastFrameMs = 0;
    private int frameCounter = 0;
    private long bitrateWindowStart = 0;
    private long bitrateWindowBytes = 0;
    private volatile int measuredFps = 0;
    private volatile int measuredKbps = 0;

    public interface FrameListener {
        void onFrame(String dataUrl, int fps, int kbps);
        void onState(boolean running, String message);
        void onBridgeState(boolean connected, String message);
    }

    public static void setFrameListener(FrameListener listener) { listenerRef = new WeakReference<>(listener); }
    public static void setBridgeUrl(String url) { bridgeUrl = url == null ? "" : url.trim().replaceAll("/+$", ""); }

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            stopCapture("Gestoppt");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {
            startForeground(NOTIFICATION_ID, buildNotification("480×234 Screen-Mirroring aktiv"));
            setBridgeUrl(intent.getStringExtra(EXTRA_BRIDGE_URL));
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent data;
            if (Build.VERSION.SDK_INT >= 33) data = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
            else data = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            if (data == null) {
                emitState(false, "Fehlende MediaProjection-Freigabe");
                stopSelf();
                return START_NOT_STICKY;
            }
            startCapture(resultCode, data);
        }
        return START_NOT_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {
        stopCapture(null);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(resultCode, data);
        if (projection == null) {
            emitState(false, "MediaProjection konnte nicht gestartet werden");
            stopSelf();
            return;
        }
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() {
                emitState(false, "Android hat die Bildschirmfreigabe beendet");
                cleanupProjection();
                stopSelf();
            }
        }, null);

        reader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(this::onImageAvailable, null);
        display = projection.createVirtualDisplay(
                "DYON-480x234",
                WIDTH, HEIGHT, 160,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, null
        );
        bitrateWindowStart = System.currentTimeMillis();
        emitState(true, "Screen-Mirroring läuft");
    }

    private void onImageAvailable(ImageReader imageReader) {
        long now = System.currentTimeMillis();
        if (now - lastFrameMs < (1000L / FPS_LIMIT)) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) image.close();
            return;
        }
        if (!frameBusy.compareAndSet(false, true)) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) image.close();
            return;
        }
        lastFrameMs = now;
        Image image = imageReader.acquireLatestImage();
        if (image == null) { frameBusy.set(false); return; }
        frameExecutor.execute(() -> {
            try { processImage(image); }
            finally { try { image.close(); } catch (Exception ignored) {} frameBusy.set(false); }
        });
    }

    private void processImage(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * WIDTH;
        int bitmapWidth = WIDTH + rowPadding / pixelStride;

        Bitmap padded = Bitmap.createBitmap(bitmapWidth, HEIGHT, Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, WIDTH, HEIGHT);
        if (cropped != padded) padded.recycle();

        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        cropped.recycle();
        byte[] jpeg = out.toByteArray();

        updateStats(jpeg.length);
        String base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP);
        FrameListener listener = listenerRef.get();
        if (listener != null) listener.onFrame("data:image/jpeg;base64," + base64, measuredFps, measuredKbps);
        uploadFrame(jpeg);
    }

    private void updateStats(int bytes) {
        long now = System.currentTimeMillis();
        frameCounter++;
        bitrateWindowBytes += bytes;
        long elapsed = now - bitrateWindowStart;
        if (elapsed >= 1000) {
            measuredFps = Math.round(frameCounter * 1000f / elapsed);
            measuredKbps = Math.round((bitrateWindowBytes * 8f) / elapsed);
            frameCounter = 0;
            bitrateWindowBytes = 0;
            bitrateWindowStart = now;
        }
    }

    private void uploadFrame(byte[] jpeg) {
        String base = bridgeUrl;
        if (base == null || base.isBlank()) return;
        if (!uploadBusy.compareAndSet(false, true)) return;
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(base + "/api/screen/frame?width=" + WIDTH + "&height=" + HEIGHT + "&format=jpeg");
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(1200);
                connection.setReadTimeout(1200);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setFixedLengthStreamingMode(jpeg.length);
                try (OutputStream os = connection.getOutputStream()) { os.write(jpeg); }
                int code = connection.getResponseCode();
                emitBridge(code >= 200 && code < 300, "HTTP " + code);
            } catch (Exception e) {
                emitBridge(false, e.getClass().getSimpleName());
            } finally {
                if (connection != null) connection.disconnect();
                uploadBusy.set(false);
            }
        });
    }

    private void emitState(boolean running, String message) {
        FrameListener listener = listenerRef.get();
        if (listener != null) listener.onState(running, message == null ? "" : message);
    }

    private void emitBridge(boolean connected, String message) {
        FrameListener listener = listenerRef.get();
        if (listener != null) listener.onBridgeState(connected, message == null ? "" : message);
    }

    private void stopCapture(String message) {
        cleanupProjection();
        if (message != null) emitState(false, message);
    }

    private void cleanupProjection() {
        ImageReader oldReader = reader;
        VirtualDisplay oldDisplay = display;
        MediaProjection oldProjection = projection;
        reader = null;
        display = null;
        projection = null;
        try { if (oldReader != null) { oldReader.setOnImageAvailableListener(null, null); oldReader.close(); } } catch (Exception ignored) {}
        try { if (oldDisplay != null) oldDisplay.release(); } catch (Exception ignored) {}
        try { if (oldProjection != null) oldProjection.stop(); } catch (Exception ignored) {}
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel("dyon_capture", "DYON Screen Mirror", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, "dyon_capture")
                : new Notification.Builder(this);
        return builder.setContentTitle("DYON Sandbox Mirror")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        cleanupProjection();
        frameExecutor.shutdownNow();
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
