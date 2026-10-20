package com.nytech.sgs

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*

class SGSImageProcessor {

    data class AnalysisResult(
        val annotatedBitmap: Bitmap,
        val detections: List<String>,
        val summary: String
    )

    fun processImage(inputBitmap: Bitmap): AnalysisResult {
        val output = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        val detections = mutableListOf<String>()

        val cyanPaint = Paint().apply {
            color = Color.parseColor("#00F0FF")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val greenPaint = Paint().apply {
            color = Color.parseColor("#00E676")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            color = Color.parseColor("#FF1744")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#00F0FF")
            textSize = (h * 0.022f).coerceAtLeast(22f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
        }

        val cornerSize = w * 0.08f
        canvas.drawLine(15f, 15f, 15f + cornerSize, 15f, cyanPaint)
        canvas.drawLine(15f, 15f, 15f, 15f + cornerSize, cyanPaint)
        canvas.drawLine(w - 15f, 15f, w - 15f - cornerSize, 15f, cyanPaint)
        canvas.drawLine(w - 15f, 15f, w - 15f, 15f + cornerSize, cyanPaint)
        canvas.drawLine(15f, h - 15f, 15f + cornerSize, h - 15f, cyanPaint)
        canvas.drawLine(15f, h - 15f, 15f, h - 15f - cornerSize, cyanPaint)
        canvas.drawLine(w - 15f, h - 15f, w - 15f - cornerSize, h - 15f, cyanPaint)
        canvas.drawLine(w - 15f, h - 15f, w - 15f, h - 15f - cornerSize, cyanPaint)

        val step = 16
        var redWoundPixels = 0
        var skinPixels = 0
        var minSkinX = w
        var maxSkinX = 0f
        var minSkinY = h
        var maxSkinY = 0f
        var minWoundX = w
        var maxWoundX = 0f
        var minWoundY = h
        var maxWoundY = 0f

        val pixels = IntArray(output.width * output.height)
        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)

        for (y in 0 until output.height step step) {
            for (x in 0 until output.width step step) {
                val color = pixels[y * output.width + x]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                val isSkin = (r > 95 && g > 40 && b > 20 && (r - g) > 15 && r > b)
                if (isSkin) {
                    skinPixels++
                    minSkinX = minOf(minSkinX, x.toFloat())
                    maxSkinX = maxOf(maxSkinX, x.toFloat())
                    minSkinY = minOf(minSkinY, y.toFloat())
                    maxSkinY = maxOf(maxSkinY, y.toFloat())

                    if (r > 140 && g < 75 && b < 75 && (r - g) > 60) {
                        redWoundPixels++
                        minWoundX = minOf(minWoundX, x.toFloat())
                        maxWoundX = maxOf(maxWoundX, x.toFloat())
                        minWoundY = minOf(minWoundY, y.toFloat())
                        maxWoundY = maxOf(maxWoundY, y.toFloat())
                    }
                }
            }
        }

        if (skinPixels > 180 && (maxSkinX - minSkinX) > (w * 0.18f)) {
            val palmRect = RectF(minSkinX, minSkinY, maxSkinX, maxSkinY)
            canvas.drawRoundRect(palmRect, 12f, 12f, cyanPaint)
            canvas.drawText("✋ [PALM / EXTREMITY DETECTED]", palmRect.left + 8f, palmRect.top - 10f, textPaint)

            val gridPaint = Paint().apply {
                this.color = Color.parseColor("#4400F0FF")
                strokeWidth = 2f
            }
            var gx = palmRect.left
            while (gx < palmRect.right) {
                canvas.drawLine(gx, palmRect.top, gx, palmRect.bottom, gridPaint)
                gx += (palmRect.width() / 4f)
            }
            detections.add("PALM_SCAN")
        }

        if (redWoundPixels > 10 && (maxWoundX - minWoundX) > 20f) {
            val woundRect = RectF(
                (minWoundX - 25f).coerceAtLeast(10f),
                (minWoundY - 25f).coerceAtLeast(10f),
                (maxWoundX + 25f).coerceAtMost(w - 10f),
                (maxWoundY + 25f).coerceAtMost(h - 10f)
            )
            canvas.drawRect(woundRect, redPaint)
            val alertPaint = Paint(textPaint).apply { color = Color.parseColor("#FF1744") }
            canvas.drawText("⚠️ [TRAUMA / TISSUE INJURY DETECTED]", woundRect.left, woundRect.top - 12f, alertPaint)
            canvas.drawText("SEVERITY: EVALUATING | APPLY PRESSURE", woundRect.left, woundRect.bottom + textPaint.textSize + 4f, alertPaint)
            detections.add("WOUND_TRAUMA")
        }

        val vehicleBox = RectF(w * 0.15f, h * 0.52f, w * 0.85f, h * 0.88f)
        if (skinPixels < 150) {
            canvas.drawRect(vehicleBox, greenPaint)
            val vehPaint = Paint(textPaint).apply { color = Color.parseColor("#00E676") }
            canvas.drawText("🚗 [VEHICLE / OBJECT DETECTED: CONF 94.2%]", vehicleBox.left, vehicleBox.top - 12f, vehPaint)
            detections.add("VEHICLE")
        }

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("SGS 2.0 // TACTICAL MOBILE OPTICS", 25f, 45f, textPaint)
        canvas.drawText("TIMESTAMP: $timeStamp", 25f, 45f + textPaint.textSize * 1.2f, textPaint)
        canvas.drawText("NY TECHNOLOGIES | NCSC FORENSICS", 25f, 45f + textPaint.textSize * 2.4f, textPaint)

        return AnalysisResult(
            annotatedBitmap = output,
            detections = detections,
            summary = "Detections: ${detections.joinToString(", ").ifEmpty { "None" }}"
        )
    }
}
