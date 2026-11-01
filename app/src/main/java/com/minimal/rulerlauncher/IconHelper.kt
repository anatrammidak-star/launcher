package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object IconHelper {

    /**
     * Mempertahankan garis luar squircle padat putih/keabuan,
     * dan mengekstrak simbol asli aplikasi (telepon, kamera, kuas, rumah, dsb.)
     * menjadi siluet/outline putih bersih di bagian tengah.
     */
    fun createMinimalistIcon(rawDrawable: Drawable, label: String): Drawable {
        val totalSize = 120
        val outputBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // 1. Gambar garis luar squircle padat putih/abu terang
        val strokeWidth = 4.5f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = Color.parseColor("#E0E0E0")
        }

        val padding = strokeWidth + 6f
        val rect = RectF(padding, padding, totalSize - padding, totalSize - padding)
        val cornerRadius = 26f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // 2. Ekstrak simbol asli aplikasi (prioritas lapisan Foreground jika Adaptive Icon)
        val symbolDrawable = extractInnerSymbol(rawDrawable)

        if (symbolDrawable != null) {
            val symbolSize = 58 // Ukuran proporsional di dalam squircle
            val symbolBitmap = renderToWhiteMask(symbolDrawable, symbolSize)
            
            val left = (totalSize - symbolSize) / 2f
            val top = (totalSize - symbolSize) / 2f
            canvas.drawBitmap(symbolBitmap, left, top, null)
        } else {
            // Cadangan jika ikon bawaan gagal diekstrak sama sekali: inisial huruf tipis
            val letter = if (label.isNotBlank()) label.trim().first().uppercaseChar().toString() else "?"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 50f
                textAlign = Paint.Align.CENTER
            }
            val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(letter, canvas.width / 2f, textY, textPaint)
        }

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Mengambil bagian simbol/gambar utama dari ikon asli
     * (Menghilangkan background padat pada AdaptiveIcon Android 8+)
     */
    private fun extractInnerSymbol(drawable: Drawable): Drawable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.foreground ?: drawable
        } else {
            drawable
        }
    }

    /**
     * Mengubah simbol asli aplikasi menjadi putih pekat / siluet putih bersih
     * dengan menghilangkan warna aslinya agar seragam.
     */
    private fun renderToWhiteMask(drawable: Drawable, targetSize: Int): Bitmap {
        val srcBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(srcCanvas)

        // Filter warna: mengubah semua piksel yang terlihat menjadi warna putih padat murni
        val colorMatrix = ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 255f, // Red
            0f, 0f, 0f, 0f, 255f, // Green
            0f, 0f, 0f, 0f, 255f, // Blue
            0f, 0f, 0f, 1f, 0f    // Alpha tetap mengikuti bentuk aslinya
        ))

        val whiteBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(whiteBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        outCanvas.drawBitmap(srcBitmap, 0f, 0f, paint)

        return whiteBitmap
    }
}
