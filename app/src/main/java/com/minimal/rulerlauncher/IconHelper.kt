package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

object IconHelper {

    fun createMinimalistIcon(rawDrawable: Drawable, label: String): Drawable {
        val totalSize = 120
        val outputBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // 1. Gambar bingkai luar squircle padat putih/abu terang
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

        // 2. Ekstrak simbol asli aplikasi dengan membuang background padat
        val extractedSymbol = extractForegroundPattern(rawDrawable, 64)

        if (extractedSymbol != null) {
            val left = (totalSize - 64) / 2f
            val top = (totalSize - 64) / 2f
            canvas.drawBitmap(extractedSymbol, left, top, null)
        } else {
            // Cadangan jika ikon bawaan transparan penuh/rusak: inisial huruf tegas
            val letter = if (label.isNotBlank()) label.trim().first().uppercaseChar().toString() else "?"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 46f
                textAlign = Paint.Align.CENTER
            }
            val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(letter, canvas.width / 2f, textY, textPaint)
        }

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Mengekstrak pola simbol dengan menghapus warna background bawaan ikon
     */
    private fun extractForegroundPattern(drawable: Drawable, targetSize: Int): Bitmap? {
        val targetDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.foreground ?: drawable
        } else {
            drawable
        }

        // Render ikon ke bitmap target
        val srcBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        targetDrawable.setBounds(0, 0, targetSize, targetSize)
        targetDrawable.draw(srcCanvas)

        val pixels = IntArray(targetSize * targetSize)
        srcBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // Deteksi warna background dari sudut-sudut ikon (kiri atas, kanan atas, kiri bawah, kanan bawah)
        val cornerColors = listOf(
            pixels[0],
            pixels[targetSize - 1],
            pixels[targetSize * (targetSize - 1)],
            pixels[targetSize * targetSize - 1]
        )

        // Ambil warna sudut yang paling dominan / tidak transparan
        val bgSample = cornerColors.firstOrNull { ((it ushr 24) and 0xFF) > 50 } ?: 0
        val hasSolidBg = ((bgSample ushr 24) and 0xFF) > 50

        val bgR = Color.red(bgSample)
        val bgG = Color.green(bgSample)
        val bgB = Color.blue(bgSample)

        val outPixels = IntArray(targetSize * targetSize)
        var foregroundPixelCount = 0

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF

            if (a < 40) {
                outPixels[i] = Color.TRANSPARENT
                continue
            }

            if (hasSolidBg) {
                // Hitung selisih jarak warna piksel terhadap warna background
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                val diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB)

                // Jika warnanya berbeda signifikan dari background, ini adalah pola/simbol intinya
                if (diff > 45) {
                    outPixels[i] = Color.WHITE
                    foregroundPixelCount++
                } else {
                    outPixels[i] = Color.TRANSPARENT
                }
            } else {
                // Jika ikon aslinya sudah berlatar transparan, langsung jadikan polanya putih
                outPixels[i] = Color.WHITE
                foregroundPixelCount++
            }
        }

        // Jika pemisahan warna gagal menghasilkan pola (misal ikon monokrom polos), kembalikan null agar dialihkan ke inisial
        if (foregroundPixelCount < 20 || foregroundPixelCount > (targetSize * targetSize * 0.85)) {
            return null
        }

        val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, targetSize, 0, 0, targetSize, targetSize)
        return resultBitmap
    }
}
