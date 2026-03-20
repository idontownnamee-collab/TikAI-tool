package com.tiktokaitool;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private EditText etKey, etPrompt;
    private Spinner spinModel;
    private Switch swAutoOpen;
    private PrefsManager prefs;
    private final String[] models = {"claude-opus-4-5","claude-sonnet-4-5","claude-haiku-4-5-20251001"};
    private final String[] labels = {"Claude Opus 4.5 (Best)","Claude Sonnet 4.5 (Fast)","Claude Haiku 4.5 (Fastest)"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        prefs = new PrefsManager(this);
        etKey    = findViewById(R.id.et_api_key);
        etPrompt = findViewById(R.id.et_prompt);
        spinModel= findViewById(R.id.spin_model);
        swAutoOpen=findViewById(R.id.sw_auto_open);

        etKey.setText(prefs.getApiKey());
        etPrompt.setText(prefs.getPrompt());
        swAutoOpen.setChecked(prefs.isAutoOpen());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinModel.setAdapter(adapter);
        String cur = prefs.getModel();
        for (int i=0;i<models.length;i++) if(models[i].equals(cur)){spinModel.setSelection(i);break;}

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset_prompt).setOnClickListener(v -> {
            prefs.setPrompt(""); etPrompt.setText(prefs.getPrompt());
        });
    }

    private void save() {
        String key = etKey.getText().toString().trim();
        if (key.isEmpty()) { Toast.makeText(this,"API key required",Toast.LENGTH_SHORT).show(); return; }
        prefs.setApiKey(key);
        prefs.setPrompt(etPrompt.getText().toString().trim());
        prefs.setModel(models[spinModel.getSelectedItemPosition()]);
        prefs.setAutoOpen(swAutoOpen.isChecked());
        Toast.makeText(this,"✓ Saved!",Toast.LENGTH_SHORT).show();
        finish();
    }
}
