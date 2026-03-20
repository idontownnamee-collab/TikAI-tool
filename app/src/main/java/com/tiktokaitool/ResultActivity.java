package com.tiktokaitool;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    private ImageView ivPreview;
    private TextView tvResult, tvStatus;
    private String imagePath, resultText;
    private PrefsManager prefs;
    private HistoryManager history;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_result);
        prefs   = new PrefsManager(this);
        history = new HistoryManager(this);
        imagePath = getIntent().getStringExtra("path");
        ivPreview = findViewById(R.id.iv_preview);
        tvResult  = findViewById(R.id.tv_result);
        tvStatus  = findViewById(R.id.tv_status);

        // Load preview with animation
        if (imagePath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            if (bmp != null) {
                ivPreview.setAlpha(0f);
                ivPreview.setImageBitmap(bmp);
                ivPreview.animate().alpha(1f).setDuration(300).start();
            }
        }

        // Check for cached result
        String cached = getIntent().getStringExtra("cached_result");
        if (cached != null && !cached.isEmpty()) {
            showResult(cached); return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_share).setOnClickListener(v -> share());
        findViewById(R.id.btn_retry).setOnClickListener(v -> doAnalyze());
        doAnalyze();
    }

    private void doAnalyze() {
        String key = prefs.getApiKey();
        if (key.isEmpty()) { tvResult.setText("No API key. Go to Settings → enter your Claude API key."); return; }
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Analyzing with Claude AI...");
        tvResult.setText("");
        tvResult.setAlpha(0f);
        findViewById(R.id.btn_share).setVisibility(View.GONE);
        findViewById(R.id.btn_retry).setVisibility(View.GONE);

        ClaudeApi.analyze(key, prefs.getModel(), prefs.getPrompt(), imagePath, new ClaudeApi.Callback() {
            @Override public void onSuccess(String r) {
                runOnUiThread(() -> { resultText=r; showResult(r); history.save(imagePath,r); });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    tvStatus.setVisibility(View.GONE);
                    tvResult.setText("Error: "+e+"\n\nTap Retry to try again.");
                    tvResult.animate().alpha(1f).setDuration(300).start();
                    findViewById(R.id.btn_retry).setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void showResult(String text) {
        tvStatus.setVisibility(View.GONE);
        tvResult.setText(text);
        tvResult.animate().alpha(1f).setDuration(400).start();
        findViewById(R.id.btn_share).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_retry).setVisibility(View.VISIBLE);
    }

    private void share() {
        if (resultText == null) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT, resultText);
        startActivity(Intent.createChooser(i, "Share analysis"));
    }
}
