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

    /**
     * Menghasilkan ikon berbingkai squircle outline padat
     * dengan simbol/pola grafis asli aplikasi di dalamnya.
     */
    fun createMinimalistIcon(rawDrawable: Drawable): Drawable {
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

        // 2. Ekstrak pola/simbol asli aplikasi
        val patternSize = 62
        val extractedPattern = extractSymbolPattern(rawDrawable, patternSize)

        val left = (totalSize - patternSize) / 2f
        val top = (totalSize - patternSize) / 2f
        canvas.drawBitmap(extractedPattern, left, top, null)

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Memproses drawable asli (termasuk AdaptiveIconDrawable pada Android 8+)
     * menjadi siluet pola grafis putih padat.
     */
    private fun extractSymbolPattern(drawable: Drawable, targetSize: Int): Bitmap {
        val targetDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            drawable.foreground ?: drawable
        } else {
            drawable
        }

        val srcBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        targetDrawable.setBounds(0, 0, targetSize, targetSize)
        targetDrawable.draw(srcCanvas)

        val pixels = IntArray(targetSize * targetSize)
        srcBitmap.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // Sampel warna background dari sudut-sudut ikon
        val corner1 = pixels[0]
        val corner2 = pixels[targetSize - 1]
        val corner3 = pixels[targetSize * (targetSize - 1)]
        val corner4 = pixels[targetSize * targetSize - 1]

        val sampleCorners = listOf(corner1, corner2, corner3, corner4)
        val solidCorner = sampleCorners.firstOrNull { ((it ushr 24) and 0xFF) > 40 }

        val outPixels = IntArray(targetSize * targetSize)

        if (solidCorner != null) {
            val bgR = Color.red(solidCorner)
            val bgG = Color.green(solidCorner)
            val bgB = Color.blue(solidCorner)

            for (i in pixels.indices) {
                val c = pixels[i]
                val a = (c ushr 24) and 0xFF

                if (a < 35) {
                    outPixels[i] = Color.TRANSPARENT
                    continue
                }

                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)
                val diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB)

                // Jika berbeda kontras dari warna sudut, maka itu adalah simbol/pola intinya
                if (diff > 40) {
                    outPixels[i] = Color.WHITE
                } else {
                    outPixels[i] = Color.TRANSPARENT
                }
            }
        } else {
            // Jika sudut transparan, ubah seluruh bentuk yang terlihat menjadi putih padat
            for (i in pixels.indices) {
                val a = (pixels[i] ushr 24) and 0xFF
                if (a > 40) {
                    outPixels[i] = Color.WHITE
                } else {
                    outPixels[i] = Color.TRANSPARENT
                }
            }
        }

        val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(outPixels, 0, targetSize, 0, 0, targetSize, targetSize)
        return resultBitmap
    }
}
