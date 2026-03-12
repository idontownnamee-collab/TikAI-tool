package com.tiktokaitool.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tiktokaitool.databinding.ActivityDownloadsBinding
import com.tiktokaitool.service.DownloadService
import com.tiktokaitool.utils.DownloadRecord
import com.tiktokaitool.utils.HistoryManager
import kotlinx.coroutines.launch

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding
    private lateinit var history: HistoryManager
    private val downloadList = mutableListOf<DownloadRecord>()

    companion object {
        private const val STORAGE_PERMISSION_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        history = HistoryManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadDownloads()

        // Handle incoming URL from MainActivity
        val incomingUrl = intent.getStringExtra("download_url")
        if (!incomingUrl.isNullOrBlank()) {
            binding.etDownloadUrl.setText(incomingUrl)
            startDownload(incomingUrl)
        }

        binding.btnStartDownload.setOnClickListener {
            val url = binding.etDownloadUrl.text.toString().trim()
            if (url.isNotBlank()) startDownload(url)
            else Toast.makeText(this, "Enter a TikTok URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        // Adapter would be set up with downloadList
    }

    private fun loadDownloads() {
        lifecycleScope.launch {
            val records = history.getDownloads()
            downloadList.clear()
            downloadList.addAll(records)
            binding.rvDownloads.adapter?.notifyDataSetChanged()
            binding.tvEmptyState.visibility =
                if (records.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun startDownload(url: String) {
        if (!checkStoragePermission()) return

        // Use DownloadService to handle the video
        val intent = android.content.Intent(this, DownloadService::class.java).apply {
            putExtra("url", url)
        }
        startForegroundService(intent)
        Toast.makeText(this, "⬇️ Download started!", Toast.LENGTH_SHORT).show()
        loadDownloads()
    }

    private fun checkStoragePermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return true // No storage permission needed for media on API 33+
        }
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
            false
        }
    }
}
