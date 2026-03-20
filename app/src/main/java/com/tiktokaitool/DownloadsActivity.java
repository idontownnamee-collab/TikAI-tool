package com.tiktokaitool;

import android.app.DownloadManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class DownloadsActivity extends AppCompatActivity {
    private EditText etUrl;
    private TextView tvStatus, tvTitle, tvType;
    private ProgressBar progress;
    private Button btnDownload;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_downloads);
        etUrl      = findViewById(R.id.et_url);
        tvStatus   = findViewById(R.id.tv_status);
        tvTitle    = findViewById(R.id.tv_title);
        progress   = findViewById(R.id.progress);
        btnDownload = findViewById(R.id.btn_download);
        tvType     = findViewById(R.id.tv_type);

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            etUrl.setText(url);
            new Handler().postDelayed(this::fetchAndDownload, 500);
        }

        btnDownload.setOnClickListener(v -> fetchAndDownload());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_paste).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
                if (t != null) {
                    etUrl.setText(t.toString());
                    Toast.makeText(this, "Pasted!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchAndDownload() {
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a TikTok URL", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.contains("tiktok") && !url.contains("vm.tiktok")) {
            Toast.makeText(this, "Not a TikTok URL", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDownload.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        tvStatus.setText("Resolving media...");
        tvTitle.setText("");
        if (tvType != null) tvType.setText("");

        ClaudeApi.getTikTokMedia(url, new ClaudeApi.VideoCallback() {
            @Override
            public void onSuccess(String mediaUrl, String title, String cover, boolean isPhoto) {
                runOnUiThread(() -> {
                    String displayTitle = title.length() > 60 ? title.substring(0, 60) + "..." : title;
                    tvTitle.setText(displayTitle);
                    if (tvType != null) {
                        tvType.setText(isPhoto ? "📸 Photo slideshow (with music)" : "🎬 Video (HD, no watermark)");
                        tvType.setVisibility(View.VISIBLE);
                    }
                    tvStatus.setText("Downloading...");
                    progress.setIndeterminate(false);
                    download(mediaUrl, title, isPhoto);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnDownload.setEnabled(true);
                    tvStatus.setText("Error: " + error);
                    Toast.makeText(DownloadsActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void download(String url, String title, boolean isPhoto) {
        try {
            String ext  = isPhoto ? ".mp4" : ".mp4"; // slideshow is still mp4 with music
            String name = "TikAI_" + System.currentTimeMillis() + ext;

            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url))
                .setTitle("TikAI: " + (title.length() > 30 ? title.substring(0, 30) : title))
                .setDescription(isPhoto ? "Photo slideshow with music" : "HD video no watermark")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "TikAI/" + name)
                .addRequestHeader("User-Agent", "Mozilla/5.0")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long dlId = dm.enqueue(req);

            // Monitor progress
            new Thread(() -> {
                boolean done = false;
                while (!done) {
                    try { Thread.sleep(800); } catch (Exception e) { break; }
                    DownloadManager.Query q = new DownloadManager.Query().setFilterById(dlId);
                    android.database.Cursor cur = dm.query(q);
                    if (cur != null && cur.moveToFirst()) {
                        int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        long total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long dl    = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        if (total > 0) {
                            int pct = (int)(dl * 100L / total);
                            runOnUiThread(() -> {
                                progress.setProgress(pct);
                                tvStatus.setText("Downloading: " + pct + "%  (" + (dl/1024/1024) + "MB)");
                            });
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            done = true;
                            runOnUiThread(() -> {
                                progress.setVisibility(View.GONE);
                                btnDownload.setEnabled(true);
                                tvStatus.setText("✅ Saved to Movies/TikAI/");
                                Toast.makeText(this, "Downloaded successfully!", Toast.LENGTH_LONG).show();
                            });
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            done = true;
                            int reason = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                            runOnUiThread(() -> {
                                progress.setVisibility(View.GONE);
                                btnDownload.setEnabled(true);
                                tvStatus.setText("Download failed (code: " + reason + "). Try again.");
                            });
                        }
                        cur.close();
                    }
                }
            }).start();

        } catch (Exception e) {
            progress.setVisibility(View.GONE);
            btnDownload.setEnabled(true);
            tvStatus.setText("Error: " + e.getMessage());
        }
    }
}
