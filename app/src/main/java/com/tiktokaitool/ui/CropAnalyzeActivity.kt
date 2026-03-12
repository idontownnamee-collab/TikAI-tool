package com.tiktokaitool.ui

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tiktokaitool.R
import com.tiktokaitool.api.ClaudeAnalyzer
import com.tiktokaitool.databinding.ActivityCropAnalyzeBinding
import com.tiktokaitool.utils.PrefsManager
import com.tiktokaitool.utils.ScreenCaptureHelper
import kotlinx.coroutines.launch

class CropAnalyzeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropAnalyzeBinding
    private lateinit var prefs: PrefsManager

    // Screen capture
    private var screenBitmap: Bitmap? = null

    // Crop selection
    private var cropRect: RectF = RectF()
    private var isDrawing = false
    private var startX = 0f
    private var startY = 0f

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            captureScreen(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screenshot permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make window fully transparent for overlay
        window.apply {
            setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        binding = ActivityCropAnalyzeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupCropView()
        setupButtons()
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        // Show instruction overlay first, then capture
        binding.tvInstruction.text = "📸 Taking screenshot..."
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun captureScreen(resultCode: Int, data: Intent) {
        lifecycleScope.launch {
            try {
                screenBitmap = ScreenCaptureHelper.capture(this@CropAnalyzeActivity, resultCode, data)
                screenBitmap?.let {
                    binding.ivScreenPreview.setImageBitmap(it)
                    binding.tvInstruction.text = "✂️ Draw a box around what you want to identify"
                    binding.cropOverlay.visibility = View.VISIBLE
                    binding.btnDone.isEnabled = false
                }
            } catch (e: Exception) {
                Toast.makeText(this@CropAnalyzeActivity,
                    "Couldn't capture screen: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupCropView() {
        binding.cropOverlay.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDrawing = true
                    startX = event.x
                    startY = event.y
                    cropRect.set(startX, startY, startX, startY)
                    binding.cropOverlay.invalidate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    cropRect.set(
                        minOf(startX, event.x),
                        minOf(startY, event.y),
                        maxOf(startX, event.x),
                        maxOf(startY, event.y)
                    )
                    binding.cropOverlay.invalidate()
                    updateCropRect()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDrawing = false
                    val width = cropRect.width()
                    val height = cropRect.height()
                    if (width > 40 && height > 40) {
                        binding.btnDone.isEnabled = true
                        binding.tvInstruction.text = "✅ Selection made! Tap DONE to analyze with AI"
                    } else {
                        binding.tvInstruction.text = "⚠️ Selection too small, try again"
                        binding.btnDone.isEnabled = false
                    }
                    true
                }
                else -> false
            }
        }

        // Make cropOverlay draw the selection rectangle
        binding.cropOverlay.apply {
            setWillNotDraw(false)
        }
    }

    private fun updateCropRect() {
        // Pass cropRect to the overlay view's draw method via tag
        binding.cropOverlay.tag = cropRect
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnDone.setOnClickListener {
            analyzeSelection()
        }

        binding.btnRetry.setOnClickListener {
            cropRect = RectF()
            binding.btnDone.isEnabled = false
            binding.tvInstruction.text = "✂️ Draw a box around what you want to identify"
            binding.cropOverlay.invalidate()
        }
    }

    private fun analyzeSelection() {
        val bitmap = screenBitmap ?: run {
            Toast.makeText(this, "No screenshot available", Toast.LENGTH_SHORT).show()
            return
        }

        if (cropRect.width() < 10 || cropRect.height() < 10) {
            Toast.makeText(this, "Please select an area first", Toast.LENGTH_SHORT).show()
            return
        }

        // Scale crop rect to bitmap coordinates
        val viewW = binding.ivScreenPreview.width.toFloat()
        val viewH = binding.ivScreenPreview.height.toFloat()
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()

        val scaleX = bmpW / viewW
        val scaleY = bmpH / viewH

        val left = (cropRect.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val top = (cropRect.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val right = (cropRect.right * scaleX).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (cropRect.bottom * scaleY).toInt().coerceIn(top + 1, bitmap.height)

        val croppedBitmap = Bitmap.createBitmap(
            bitmap, left, top,
            right - left,
            bottom - top
        )

        // Show loading
        binding.btnDone.isEnabled = false
        binding.tvInstruction.text = "🤖 AI is analyzing your selection..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val apiKey = prefs.getApiKey()
                val analyzer = ClaudeAnalyzer(apiKey)
                val result = analyzer.analyzeImage(croppedBitmap, prefs.getAnalysisPrompt())

                // Launch result screen
                val intent = Intent(this@CropAnalyzeActivity, ResultActivity::class.java).apply {
                    putExtra("analysis_result", result)
                    putExtra("cropped_image_path", saveTempBitmap(croppedBitmap))
                }
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnDone.isEnabled = true
                binding.tvInstruction.text = "❌ Error: ${e.message}"
                Toast.makeText(this@CropAnalyzeActivity,
                    "Analysis failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveTempBitmap(bitmap: Bitmap): String {
        val file = java.io.File(cacheDir, "crop_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        return file.absolutePath
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
