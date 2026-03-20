package com.tiktokaitool;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.*;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.io.*;
import java.nio.ByteBuffer;

public class OverlayService extends Service {
    public static boolean isRunning = false;
    public static MediaProjection sProjection = null;
    private WindowManager wm;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final String CH = "tikai_ch";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if ("CAPTURE".equals(action)) {
            // Called after projection is ready
            handler.postDelayed(this::doCapture, 300);
            return START_STICKY;
        }

        if ("SHOW".equals(action)) {
            // Re-show overlay button
            if (overlayView != null) {
                handler.post(() -> overlayView.animate().alpha(1f).setDuration(200).start());
            }
            return START_STICKY;
        }

        // First start - setup everything
        createChannel();
        startForeground(1, buildNotif());
        if (overlayView == null) showOverlay();
        isRunning = true;
        return START_STICKY;
    }

    private void showOverlay() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_button, null);

        overlayParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        overlayParams.x = 0;
        overlayParams.y = 0;

        final int[] lastY = {0};
        final long[] downTime = {0};
        final float[] startY = {0};

        overlayView.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastY[0] = (int) e.getRawY();
                    startY[0] = e.getRawY();
                    downTime[0] = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dy = (int) e.getRawY() - lastY[0];
                    overlayParams.y += dy;
                    wm.updateViewLayout(overlayView, overlayParams);
                    lastY[0] = (int) e.getRawY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float totalMove = Math.abs(e.getRawY() - startY[0]);
                    long elapsed = System.currentTimeMillis() - downTime[0];
                    // Tap = small move + short time
                    if (totalMove < 15 && elapsed < 300) {
                        onFindTapped();
                    }
                    return true;
            }
            return false;
        });

        try {
            wm.addView(overlayView, overlayParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onFindTapped() {
        // Hide overlay
        if (overlayView != null) {
            overlayView.animate().alpha(0f).setDuration(150).start();
        }

        handler.postDelayed(() -> {
            if (sProjection != null) {
                // Already have permission, capture directly
                doCapture();
            } else {
                // Need to ask for screen capture permission
                Intent i = new Intent(this, ScreenCaptureActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            }
        }, 300);
    }

    private void doCapture() {
        if (sProjection == null) {
            handler.post(() -> {
                if (overlayView != null) overlayView.animate().alpha(1f).setDuration(200).start();
                Toast.makeText(this, "Screen capture not available", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        int w = dm.widthPixels, h = dm.heightPixels, dpi = dm.densityDpi;

        try {
            ImageReader reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2);
            VirtualDisplay vd = sProjection.createVirtualDisplay(
                "TikAI", w, h, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, null);

            reader.setOnImageAvailableListener(r -> {
                Image img = null;
                try {
                    img = r.acquireLatestImage();
                    if (img == null) return;

                    Image.Plane[] planes = img.getPlanes();
                    ByteBuffer buf = planes[0].getBuffer();
                    int ps = planes[0].getPixelStride();
                    int rs = planes[0].getRowStride();
                    int pad = (rs - ps * w) / ps;

                    Bitmap bmp = Bitmap.createBitmap(w + pad, h, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(buf);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, w, h);

                    vd.release();
                    reader.close();

                    // Save screenshot
                    File dir = new File(getCacheDir(), "caps");
                    dir.mkdirs();
                    File f = new File(dir, "cap_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fo = new FileOutputStream(f)) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, fo);
                    }

                    final String path = f.getAbsolutePath();
                    handler.post(() -> {
                        // Show overlay again
                        if (overlayView != null) overlayView.animate().alpha(1f).setDuration(200).start();
                        // Open crop screen
                        Intent intent = new Intent(this, CropActivity.class);
                        intent.putExtra("path", path);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> {
                        if (overlayView != null) overlayView.animate().alpha(1f).setDuration(200).start();
                        Toast.makeText(this, "Capture failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } finally {
                    if (img != null) img.close();
                }
            }, handler);

        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> {
                if (overlayView != null) overlayView.animate().alpha(1f).setDuration(200).start();
                // Projection may be invalid, reset it
                sProjection = null;
                Toast.makeText(this, "Capture error, tap FIND again", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CH, "TikAI Overlay", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotif() {
        Intent stop = new Intent(this, OverlayService.class);
        stop.setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 0, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CH)
            .setContentTitle("TikAI Active")
            .setContentText("Tap FIND button to analyze items")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .addAction(android.R.drawable.ic_delete, "Stop", pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (overlayView != null) {
            try { wm.removeView(overlayView); } catch (Exception e) { e.printStackTrace(); }
            overlayView = null;
        }
        if (sProjection != null) {
            try { sProjection.stop(); } catch (Exception e) { e.printStackTrace(); }
            sProjection = null;
        }
    }
}
