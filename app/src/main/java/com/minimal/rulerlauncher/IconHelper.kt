package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object IconHelper {

    fun toMinimalistOutline(drawable: Drawable): Drawable {
        val size = 96
        val srcBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val srcCanvas = Canvas(srcBitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(srcCanvas)

        val outBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        srcBitmap.getPixels(pixels, 0, size, 0, 0, size, size)

        val outPixels = IntArray(size * size)

        // Deteksi tepi (edge detection) untuk membuat garis outline putih
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val idx = y * size + x
                val alpha = (pixels[idx] ushr 24) and 0xFF

                if (alpha > 50) {
                    val aLeft = (pixels[idx - 1] ushr 24) and 0xFF
                    val aRight = (pixels[idx + 1] ushr 24) and 0xFF
                    val aTop = (pixels[idx - size] ushr 24) and 0xFF
                    val aBottom = (pixels[idx + size] ushr 24) and 0xFF

                    // Jika berada di perbatasan piksel atau kontur detail
                    val isEdge = (aLeft < 50 || aRight < 50 || aTop < 50 || aBottom < 50)
                    
                    // Juga deteksi perbedaan kontras di dalam gambar
                    val lum = (Color.red(pixels[idx]) + Color.green(pixels[idx]) + Color.blue(pixels[idx])) / 3
                    val lumRight = (Color.red(pixels[idx + 1]) + Color.green(pixels[idx + 1]) + Color.blue(pixels[idx + 1])) / 3
                    val isContrast = Math.abs(lum - lumRight) > 35

                    if (isEdge || isContrast) {
                        outPixels[idx] = Color.WHITE
                    }
                }
            }
        }

        outBitmap.setPixels(outPixels, 0, size, 0, 0, size, size)
        return BitmapDrawable(null, outBitmap)
    }
}
