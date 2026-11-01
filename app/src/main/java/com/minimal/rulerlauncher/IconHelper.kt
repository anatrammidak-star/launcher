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

        // 1. Gambar bingkai garis luar squircle padat putih/abu terang
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

        // 2. Ekstrak pola grafis asli aplikasi
        val innerPattern = extractShapePattern(rawDrawable, 60)

        val left = (totalSize - 60) / 2f
        val top = (totalSize - 60) / 2f
        canvas.drawBitmap(innerPattern, left, top, null)

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Mengekstrak pola/bentuk grafis asli aplikasi menjadi siluet putih padat
     */
    private fun extractShapePattern(drawable: Drawable, targetSize: Int): Bitmap {
        // Ambil layer foreground jika Adaptive Icon, agar terbebas dari base-layer sistem
        val targetDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.foreground ?: drawable
        } else {
            drawable
        }

        // Render ikon ke bitmap berukuran target
        val srcBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        targetDrawable.setBounds(0, 0, targetSize, targetSize)
        targetDrawable.draw(srcCanvas)

        val pixels = IntArray(targetSize * targetSize)
        srcBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // Hitung rata-rata kecerahan (luminance) piksel yang terlihat
        var totalLum = 0L
        var visibleCount = 0
        for (c in pixels) {
            val a = (c ushr 24) and 0xFF
            if (a > 30) {
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                totalLum += (r * 299 + g * 587 + b * 114) / 1000
                visibleCount++
            }
        }
        val avgLum = if (visibleCount > 0) (totalLum / visibleCount).toInt() else 128

        // Deteksi apakah simbol aplikasi biasanya lebih terang dari latar (seperti logo putih WhatsApp)
        // atau simbolnya lebih gelap dari latar
        val sampleCorner = pixels[0]
        val cornerA = (sampleCorner ushr 24) and 0xFF
        val cornerLum = if (cornerA > 30) {
            (Color.red(sampleCorner) * 299 + Color.green(sampleCorner) * 587 + Color.blue(sampleCorner) * 114) / 1000
        } else {
            -1
        }

        val outPixels = IntArray(targetSize * targetSize)

        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF

            if (a < 30) {
                outPixels[i] = Color.TRANSPARENT
                continue
            }

            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val lum = (r * 299 + g * 587 + b * 114) / 1000

            val isPattern = if (cornerLum != -1) {
                // Jika sudut memiliki warna latar belakang, pisahkan pola berdasarkan perbedaan luminansi dari sudut
                Math.abs(lum - cornerLum) > 35
            } else {
                // Jika sudut transparan, seluruh bentuk terlihat adalah pola itu sendiri
                true
            }

            if (isPattern) {
                outPixels[i] = Color.WHITE
            } else {
                outPixels[i] = Color.TRANSPARENT
            }
        }

        val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, targetSize, 0, 0, targetSize, targetSize)
        return resultBitmap
    }
}
