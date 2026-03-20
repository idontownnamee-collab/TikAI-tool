package com.tiktokaitool;

import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tiktokaitool.databinding.ActivityDownloadsBinding;
import com.tiktokaitool.databinding.BottomsheetQualityBinding;

public class DownloadsActivity extends AppCompatActivity {

    private ActivityDownloadsBinding binding;
    private String resolvedVideoUrl = "";
    private String resolvedAudioUrl = "";
    private String resolvedHdUrl    = "";
    private String resolvedTitle    = "";
    private String resolvedCover    = "";
    private boolean resolvedIsPhoto = false;
    private boolean isResolved      = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        binding = ActivityDownloadsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            binding.etUrl.setText(url);
            new Handler().postDelayed(this::fetchMedia, 400);
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPaste.setOnClickListener(v -> paste());
        binding.btnFetch.setOnClickListener(v -> fetchMedia());
        binding.btnDownload.setOnClickListener(v -> showQualitySheet());
    }

    private void paste() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
            CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
            if (t != null) { binding.etUrl.setText(t.toString()); fetchMedia(); }
        }
    }

    private void fetchMedia() {
        String url = binding.etUrl.getText().toString().trim();
        if (url.isEmpty()) { Toast.makeText(this,"Enter a TikTok URL",Toast.LENGTH_SHORT).show(); return; }

        isResolved = false;
        binding.btnDownload.setEnabled(false);
        binding.layoutResult.setVisibility(View.GONE);
        binding.progressFetch.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("Resolving media info...");

        ClaudeApi.getTikTokMedia(url, new ClaudeApi.VideoCallback() {
            @Override
            public void onSuccess(String videoUrl, String title, String cover, boolean isPhoto) {
                runOnUiThread(() -> {
                    resolvedVideoUrl = videoUrl;
                    resolvedHdUrl    = videoUrl;
                    resolvedTitle    = title;
                    resolvedCover    = cover;
                    resolvedIsPhoto  = isPhoto;
                    isResolved = true;

                    binding.progressFetch.setVisibility(View.GONE);
                    binding.layoutResult.setVisibility(View.VISIBLE);
                    binding.tvTitle.setText(title.length() > 70 ? title.substring(0,70)+"..." : title);
                    binding.tvMediaType.setText(isPhoto ? "📸 Photo slideshow with music" : "🎬 Video");
                    binding.tvStatus.setText("Ready to download — choose quality below");
                    binding.btnDownload.setEnabled(true);

                    // Animate result card in
                    binding.layoutResult.setAlpha(0f);
                    binding.layoutResult.setTranslationY(20f);
                    binding.layoutResult.animate().alpha(1f).translationY(0f).setDuration(300).start();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressFetch.setVisibility(View.GONE);
                    binding.tvStatus.setText("Error: " + error);
                    Toast.makeText(DownloadsActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showQualitySheet() {
        if (!isResolved) return;

        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        BottomsheetQualityBinding sb = BottomsheetQualityBinding.inflate(getLayoutInflater());
        sheet.setContentView(sb.getRoot());

        sb.optionHd.setOnClickListener(v -> {
            sheet.dismiss();
            startDownload(resolvedHdUrl, DownloadHelper.Quality.HD_VIDEO);
        });
        sb.optionNowatermark.setOnClickListener(v -> {
            sheet.dismiss();
            startDownload(resolvedVideoUrl, DownloadHelper.Quality.NO_WATERMARK);
        });
        sb.optionMp3.setOnClickListener(v -> {
            sheet.dismiss();
            // For MP3 we use the same URL but DownloadHelper extracts audio
            startDownload(resolvedVideoUrl, DownloadHelper.Quality.MP3_AUDIO);
        });

        sheet.show();
    }

    private void startDownload(String url, DownloadHelper.Quality quality) {
        binding.progressDownload.setVisibility(View.VISIBLE);
        binding.progressDownload.setProgress(0);
        binding.btnDownload.setEnabled(false);
        binding.tvStatus.setText("Starting download...");

        DownloadHelper.download(this, url, resolvedTitle,
            resolvedIsPhoto, quality, new DownloadHelper.ProgressCallback() {

            @Override
            public void onProgress(int pct, long dlMB, long totalMB) {
                runOnUiThread(() -> {
                    binding.progressDownload.setProgress(pct);
                    String label = quality == DownloadHelper.Quality.MP3_AUDIO ? "MP3" :
                                   quality == DownloadHelper.Quality.HD_VIDEO ? "HD Video" : "Video";
                    binding.tvStatus.setText(label + " • " + pct + "% • " + dlMB + "/" + totalMB + " MB");
                });
            }

            @Override
            public void onSuccess(String filePath, String fileName) {
                runOnUiThread(() -> {
                    binding.progressDownload.setProgress(100);
                    binding.btnDownload.setEnabled(true);
                    binding.tvStatus.setText("✅ Saved: " + fileName);
                    Toast.makeText(DownloadsActivity.this,
                        "Downloaded! Check your Gallery/Files", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressDownload.setVisibility(View.GONE);
                    binding.btnDownload.setEnabled(true);
                    binding.tvStatus.setText("Download failed: " + error);
                });
            }
        });
    }
}
