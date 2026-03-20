package com.tiktokaitool;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadHelper {

    public enum Quality { HD_VIDEO, NO_WATERMARK, MP3_AUDIO }

    public interface ProgressCallback {
        void onProgress(int percent, long downloadedMB, long totalMB);
        void onSuccess(String filePath, String fileName);
        void onError(String message);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void download(Context ctx, String url, String title,
                                boolean isPhoto, Quality quality, ProgressCallback cb) {
        executor.execute(() -> {
            try {
                String ext      = quality == Quality.MP3_AUDIO ? ".mp3" : ".mp4";
                String mimeType = quality == Quality.MP3_AUDIO ? "audio/mpeg" : "video/mp4";
                String dir      = quality == Quality.MP3_AUDIO
                    ? Environment.DIRECTORY_MUSIC
                    : (isPhoto ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_MOVIES);
                String subDir   = "TikAI";
                String fileName = "TikAI_" + System.currentTimeMillis() + ext;

                Uri fileUri;
                OutputStream out;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped Storage — MediaStore API
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    cv.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                    cv.put(MediaStore.MediaColumns.RELATIVE_PATH, dir + File.separator + subDir);
                    cv.put(MediaStore.MediaColumns.IS_PENDING, 1);

                    Uri collection;
                    if (quality == Quality.MP3_AUDIO) {
                        collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    } else if (isPhoto) {
                        collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        cv.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4"); // slideshow is video
                        collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    } else {
                        collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    }

                    fileUri = ctx.getContentResolver().insert(collection, cv);
                    if (fileUri == null) { cb.onError("Could not create file"); return; }
                    out = ctx.getContentResolver().openOutputStream(fileUri);
                } else {
                    // Legacy storage
                    File folder = new File(Environment.getExternalStoragePublicDirectory(dir), subDir);
                    folder.mkdirs();
                    File f = new File(folder, fileName);
                    fileUri = Uri.fromFile(f);
                    out = new FileOutputStream(f);
                }

                // Stream download
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.connect();

                long total = conn.getContentLengthLong();
                long downloaded = 0;
                byte[] buf = new byte[8192];
                int n;

                try (InputStream in = conn.getInputStream()) {
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        downloaded += n;
                        if (total > 0) {
                            int pct = (int)(downloaded * 100 / total);
                            long dlMB = downloaded / 1024 / 1024;
                            long totMB = total / 1024 / 1024;
                            cb.onProgress(pct, dlMB, totMB);
                        }
                    }
                }
                out.close();

                // Mark as complete (Scoped Storage)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv2 = new ContentValues();
                    cv2.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    ctx.getContentResolver().update(fileUri, cv2, null, null);
                }

                // Scan file so it appears in Gallery immediately
                final Uri scanUri = fileUri;
                final String scanPath = fileUri.toString();
                MediaScannerConnection.scanFile(ctx,
                    new String[]{scanUri.toString()},
                    new String[]{mimeType},
                    (path, uri) -> {}
                );

                cb.onSuccess(fileUri.toString(), fileName);

            } catch (Exception e) {
                cb.onError(e.getMessage() != null ? e.getMessage() : "Download failed");
            }
        });
    }
}
