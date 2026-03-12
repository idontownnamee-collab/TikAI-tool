package com.tiktokaitool.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tiktokaitool.api.ClaudeAnalyzer
import com.tiktokaitool.databinding.ActivitySettingsBinding
import com.tiktokaitool.utils.PrefsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Load current values
        binding.etApiKey.setText(prefs.getApiKey())
        binding.etCustomPrompt.setText(prefs.getAnalysisPrompt().ifBlank { ClaudeAnalyzer.DEFAULT_PROMPT })
        binding.switchOverlay.isChecked = prefs.isOverlayEnabled()
        binding.switchAutoOverlay.isChecked = prefs.isAutoOverlayOnTikTok()

        // Focus on API key if coming from warning
        if (intent.getBooleanExtra("focus_api_key", false)) {
            binding.etApiKey.requestFocus()
        }

        binding.btnSaveSettings.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val prompt = binding.etCustomPrompt.text.toString().trim()

            if (apiKey.isBlank()) {
                Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.saveApiKey(apiKey)
            prefs.saveAnalysisPrompt(prompt)
            prefs.setOverlayEnabled(binding.switchOverlay.isChecked)
            prefs.setAutoOverlayOnTikTok(binding.switchAutoOverlay.isChecked)

            Toast.makeText(this, "✅ Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnResetPrompt.setOnClickListener {
            binding.etCustomPrompt.setText(ClaudeAnalyzer.DEFAULT_PROMPT)
            Toast.makeText(this, "Prompt reset to default", Toast.LENGTH_SHORT).show()
        }

        binding.tvGetApiKey.setOnClickListener {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://console.anthropic.com/")
            )
            startActivity(intent)
        }
    }
}
