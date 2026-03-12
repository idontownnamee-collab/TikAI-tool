package com.tiktokaitool.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tiktokaitool.R
import com.tiktokaitool.databinding.ActivityMainBinding
import com.tiktokaitool.service.OverlayService
import com.tiktokaitool.utils.PrefsManager
import com.tiktokaitool.utils.TikTokUtils

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
        binding.btnOpenTiktok.setOnClickListener { openTikTok() }

        binding.btnAiFinder.setOnClickListener {
            if (prefs.getApiKey().isBlank()) {
                showApiKeyDialog(); return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission(); return@setOnClickListener
            }
            openTikTokWithOverlay()
        }

        binding.btnDownloads.setOnClickListener {
            startActivity(Intent(this, DownloadsActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnPasteDownload.setOnClickListener {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (clip.contains("tiktok.com") || clip.contains("vm.tiktok")) {
                binding.etVideoUrl.setText(clip)
                Toast.makeText(this, "Pasted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No TikTok link in clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDownloadVideo.setOnClickListener {
            val url = binding.etVideoUrl.text.toString().trim()
            if (url.isNotBlank()) startDownload(url)
            else Toast.makeText(this, "Enter a TikTok URL first", Toast.LENGTH_SHORT).show()
        }

        updateOverlayStatus()
    }

    private fun openTikTokWithOverlay() {
        startOverlayService()
        Handler(Looper.getMainLooper()).postDelayed({ openTikTok() }, 400)
        Toast.makeText(this, "FIND button is active on the left side of TikTok!", Toast.LENGTH_LONG).show()
    }

    private fun openTikTok() {
        val pm = packageManager
        val pkg = when {
            isAppInstalled(pm, TIKTOK_PACKAGE_MUSICALLY) -> TIKTOK_PACKAGE_MUSICALLY
            isAppInstalled(pm, TIKTOK_PACKAGE_TRILL) -> TIKTOK_PACKAGE_TRILL
            else -> null
        }
        if (pkg != null) {
            val intent = pm.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (intent != null) startActivity(intent)
            else Toast.makeText(this, "Could not launch TikTok", Toast.LENGTH_SHORT).show()
        } else {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$TIKTOK_PACKAGE_MUSICALLY"))) }
            catch (e: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$TIKTOK_PACKAGE_MUSICALLY"))) }
        }
    }

    private fun startOverlayService() {
        if (Settings.canDrawOverlays(this))
            startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun startDownload(url: String) {
        if (!TikTokUtils.isValidTikTokUrl(url)) {
            Toast.makeText(this, "Invalid TikTok URL", Toast.LENGTH_SHORT).show(); return
        }
        startActivity(Intent(this, DownloadsActivity::class.java).putExtra("download_url", url))
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permission Required")
            .setMessage("TikAI needs Display over other apps permission to show the FIND button while you watch TikTok.")
            .setPositiveButton("Grant") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    OVERLAY_PERMISSION_CODE)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showApiKeyDialog() {
        startActivity(Intent(this, SettingsActivity::class.java).putExtra("focus_api_key", true))
        Toast.makeText(this, "Set your Claude API key first", Toast.LENGTH_LONG).show()
    }

    private fun checkApiKey() {
        binding.cardApiWarning.visibility = if (prefs.getApiKey().isBlank()) View.VISIBLE else View.GONE
    }

    private fun updateOverlayStatus() {
        val active = Settings.canDrawOverlays(this) && OverlayService.isRunning
        binding.tvOverlayStatus.text = if (active) "AI Overlay: Active" else "AI Overlay: Inactive"
        binding.tvOverlayStatus.setTextColor(ContextCompat.getColor(this,
            if (active) R.color.green_active else R.color.text_secondary))
    }

    private fun isAppInstalled(pm: PackageManager, pkg: String) = try {
        pm.getPackageInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) { false }

    override fun onResume() { super.onResume(); updateOverlayStatus(); checkApiKey() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) openTikTokWithOverlay()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}