package com.tiktokaitool.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tiktokaitool.databinding.ActivityResultBinding
import io.noties.markwon.Markwon

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val result = intent.getStringExtra("analysis_result") ?: "No result"
        val imgPath = intent.getStringExtra("cropped_image_path")

        // Show cropped image
        imgPath?.let {
            try {
                val bmp = BitmapFactory.decodeFile(it)
                binding.ivCroppedPreview.setImageBitmap(bmp)
            } catch (e: Exception) {
                // ignore
            }
        }

        // Render markdown result
        try {
            val markwon = Markwon.create(this)
            markwon.setMarkdown(binding.tvResult, result)
        } catch (e: Exception) {
            binding.tvResult.text = result
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCopyResult.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("AI Result", result))
            Toast.makeText(this, "✅ Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareResult.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "🤖 TikAI Finder Result:\n\n$result")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Result"))
        }

        binding.btnFindAgain.setOnClickListener {
            startActivity(Intent(this, CropAnalyzeActivity::class.java))
            finish()
        }

        binding.btnSaveHistory.setOnClickListener {
            // Save to history via PrefsManager
            com.tiktokaitool.utils.HistoryManager(this).saveEntry(result, imgPath)
            Toast.makeText(this, "✅ Saved to history!", Toast.LENGTH_SHORT).show()
            binding.btnSaveHistory.isEnabled = false
            binding.btnSaveHistory.text = "✅ Saved"
        }
    }
}
