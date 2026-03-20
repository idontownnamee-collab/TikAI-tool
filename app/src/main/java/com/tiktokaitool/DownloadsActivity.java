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
    private TextView tvStatus, tvTitle;
    private ProgressBar progress;
    private Button btnDownload;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_downloads);
        etUrl      = findViewById(R.id.et_url);
        tvStatus   = findViewById(R.id.tv_status);
        tvTitle    = findViewById(R.id.tv_title);
        progress   = findViewById(R.id.progress);
        btnDownload = findViewById(R.id.btn_download);

        String url = getIntent().getStringExtra("url");
        if (url != null) { etUrl.setText(url); new Handler().postDelayed(this::fetchAndDownload, 400); }

        btnDownload.setOnClickListener(v -> fetchAndDownload());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_paste).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() != false && cm.getPrimaryClip() != null) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
                if (t != null) etUrl.setText(t.toString());
            }
        });
    }

    private void fetchAndDownload() {
        String url = etUrl.getText().toString().trim();
        if (url.isEmpty()) { Toast.makeText(this,"Enter a TikTok URL",Toast.LENGTH_SHORT).show(); return; }
        btnDownload.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);
        tvStatus.setText("Resolving video...");
        tvTitle.setText("");

        ClaudeApi.getTikTokVideo(url, new ClaudeApi.VideoCallback() {
            @Override public void onSuccess(String videoUrl, String title, String cover) {
                runOnUiThread(() -> {
                    tvTitle.setText(title.length()>60?title.substring(0,60)+"...":title);
                    tvStatus.setText("Starting download...");
                    progress.setIndeterminate(false);
                    download(videoUrl, title);
                });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnDownload.setEnabled(true);
                    tvStatus.setText("Error: "+e);
                    Toast.makeText(DownloadsActivity.this,"Failed: "+e,Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void download(String url, String title) {
        try {
            String name = "TikAI_"+System.currentTimeMillis()+".mp4";
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url))
                .setTitle("TikAI: "+(title.length()>30?title.substring(0,30):title))
                .setDescription("Downloading no-watermark video")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "TikAI/"+name)
                .addRequestHeader("User-Agent","Mozilla/5.0")
                .setAllowedOverMetered(true).setAllowedOverRoaming(true);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long dlId = dm.enqueue(req);

            new Thread(() -> {
                boolean done = false;
                while (!done) {
                    try { Thread.sleep(1000); } catch (Exception e) { break; }
                    DownloadManager.Query q = new DownloadManager.Query().setFilterById(dlId);
                    android.database.Cursor cur = dm.query(q);
                    if (cur.moveToFirst()) {
                        int status = cur.getInt(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                        long total = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        long dl    = cur.getLong(cur.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        if (total > 0) {
                            int pct = (int)(dl*100L/total);
                            runOnUiThread(() -> { progress.setProgress(pct); tvStatus.setText("Downloading: "+pct+"%"); });
                        }
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            done = true;
                            runOnUiThread(() -> {
                                progress.setVisibility(View.GONE); btnDownload.setEnabled(true);
                                tvStatus.setText("✓ Saved to Movies/TikAI/");
                                Toast.makeText(this,"Video saved!",Toast.LENGTH_LONG).show();
                            });
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            done = true;
                            runOnUiThread(() -> {
                                progress.setVisibility(View.GONE); btnDownload.setEnabled(true);
                                tvStatus.setText("Download failed. Try again.");
                            });
                        }
                    }
                    cur.close();
                }
            }).start();
        } catch(Exception e) {
            progress.setVisibility(View.GONE); btnDownload.setEnabled(true);
            tvStatus.setText("Error: "+e.getMessage());
        }
    }
}
