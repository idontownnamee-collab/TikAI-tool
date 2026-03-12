package com.tiktokaitool.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val dimPaint = Paint().apply {
        color = Color.argb(120, 0, 0, 0)
    }

    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(15f, 8f), 0f)
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val labelBg = Paint().apply {
        color = Color.argb(200, 0, 120, 255)
    }

    private val labelText = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    var cropRect: RectF = RectF()
        set(value) {
            field = value
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (cropRect.isEmpty) {
            // Draw full dim
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
            return
        }

        // Draw dim outside crop
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        // Clear inside crop (transparent window)
        canvas.drawRect(cropRect, clearPaint)

        // Draw dashed border
        canvas.drawRect(cropRect, borderPaint)

        // Draw corner handles
        val cLen = 30f
        val l = cropRect.left
        val t = cropRect.top
        val r = cropRect.right
        val b = cropRect.bottom

        // Top-left
        canvas.drawLine(l, t, l + cLen, t, cornerPaint)
        canvas.drawLine(l, t, l, t + cLen, cornerPaint)
        // Top-right
        canvas.drawLine(r - cLen, t, r, t, cornerPaint)
        canvas.drawLine(r, t, r, t + cLen, cornerPaint)
        // Bottom-left
        canvas.drawLine(l, b - cLen, l, b, cornerPaint)
        canvas.drawLine(l, b, l + cLen, b, cornerPaint)
        // Bottom-right
        canvas.drawLine(r - cLen, b, r, b, cornerPaint)
        canvas.drawLine(r, b - cLen, r, b, cornerPaint)

        // Size label
        val w = cropRect.width().toInt()
        val h = cropRect.height().toInt()
        val label = " ${w}×${h} "
        val labelW = labelText.measureText(label)
        val labelX = cropRect.left
        val labelY = cropRect.top - 10f

        if (labelY > 40f) {
            canvas.drawRoundRect(
                labelX, labelY - 36f,
                labelX + labelW, labelY + 4f,
                8f, 8f, labelBg
            )
            canvas.drawText(label, labelX, labelY, labelText)
        }
    }
}
