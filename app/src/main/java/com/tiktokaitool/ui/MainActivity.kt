package com.tiktokaitool.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiktokaitool.R
import com.tiktokaitool.databinding.ActivityMainBinding
import com.tiktokaitool.service.OverlayService
import com.tiktokaitool.utils.PrefsManager
import com.tiktokaitool.utils.TikTokUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager

    companion object {
        private const val OVERLAY_PERMISSION_CODE = 1001
        private const val TIKTOK_PACKAGE_MUSICALLY = "com.zhiliaoapp.musically"
        private const val TIKTOK_PACKAGE_TRILL = "com.ss.android.ugc.trill"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupUI()
        checkApiKey()
    }

    private fun setupUI() {
        // ─── Open TikTok Button ───
        binding.btnOpenTiktok.setOnClickListener {
            openTikTok()
        }

        // ─── AI Finder Button ───
        binding.btnAiFinder.setOnClickListener {
            if (prefs.getApiKey().isBlank()) {
                showApiKeyDialog()
            } else {
                startAIOverlay()
            }
        }

        // ─── Downloads Button ───
        binding.btnDownloads.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }

        // ─── Settings Button ───
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ─── Paste & Download ───
        binding.btnPasteDownload.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (clip.contains("tiktok.com") || clip.contains("vm.tiktok")) {
                binding.etVideoUrl.setText(clip)
                Toast.makeText(this, "✅ TikTok link pasted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ No TikTok link in clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDownloadVideo.setOnClickListener {
            val url = binding.etVideoUrl.text.toString().trim()
            if (url.isNotBlank()) {
                startDownload(url)
            } else {
                Toast.makeText(this, "Enter or paste a TikTok URL first", Toast.LENGTH_SHORT).show()
            }
        }

        // ─── Status Card ───
        updateOverlayStatus()
    }

    private fun openTikTok() {
        val pm = packageManager
        val tiktokPackage = when {
            isAppInstalled(pm, TIKTOK_PACKAGE_MUSICALLY) -> TIKTOK_PACKAGE_MUSICALLY
            isAppInstalled(pm, TIKTOK_PACKAGE_TRILL) -> TIKTOK_PACKAGE_TRILL
            else -> null
        }

        if (tiktokPackage != null) {
            val intent = pm.getLaunchIntentForPackage(tiktokPackage)
            if (intent != null) {
                startActivity(intent)
                // If overlay is enabled, start it
                if (prefs.isOverlayEnabled() && Settings.canDrawOverlays(this)) {
                    startOverlayService()
                }
            }
        } else {
            // TikTok not installed → open Play Store
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$TIKTOK_PACKAGE_MUSICALLY")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$TIKTOK_PACKAGE_MUSICALLY")))
            }
        }
    }

    private fun startAIOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        startOverlayService()
        openTikTok()
        Toast.makeText(this, "🤖 AI Finder is ACTIVE — look for the 'FIND' button!", Toast.LENGTH_LONG).show()
        updateOverlayStatus()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startForegroundService(intent)
    }

    private fun startDownload(url: String) {
        if (!TikTokUtils.isValidTikTokUrl(url)) {
            Toast.makeText(this, "Invalid TikTok URL", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, DownloadsActivity::class.java).apply {
            putExtra("download_url", url)
        }
        startActivity(intent)
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("TikAI Tool needs 'Display over other apps' permission to show the AI Finder button while you watch TikTok.")
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showApiKeyDialog() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("focus_api_key", true)
        }
        startActivity(intent)
        Toast.makeText(this, "Please set your Claude API key first", Toast.LENGTH_LONG).show()
    }

    private fun checkApiKey() {
        if (prefs.getApiKey().isBlank()) {
            binding.cardApiWarning.visibility = View.VISIBLE
        } else {
            binding.cardApiWarning.visibility = View.GONE
        }
    }

    private fun updateOverlayStatus() {
        val active = Settings.canDrawOverlays(this) &&
                OverlayService.isRunning
        binding.tvOverlayStatus.text = if (active) "🟢 AI Overlay: Active" else "⚫ AI Overlay: Inactive"
        binding.tvOverlayStatus.setTextColor(
            ContextCompat.getColor(this,
                if (active) R.color.green_active else R.color.text_secondary)
        )
    }

    private fun isAppInstalled(pm: PackageManager, pkg: String): Boolean {
        return try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayStatus()
        checkApiKey()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "✅ Permission granted!", Toast.LENGTH_SHORT).show()
                startAIOverlay()
            } else {
                Toast.makeText(this, "❌ Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
