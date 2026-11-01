package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object IconHelper {

    /**
     * Mengubah ikon aplikasi apa pun menjadi ikon monokrom putih minimalis
     */
    fun toMinimalistIcon(drawable: Drawable): Drawable {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        // Matriks warna: Mengubah piksel berwarna menjadi grayscale lalu mencerahkannya menjadi putih
        val colorMatrix = ColorMatrix(floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 150f,
            0.33f, 0.33f, 0.33f, 0f, 150f,
            0.33f, 0.33f, 0.33f, 0f, 150f,
            0f,    0f,    0f,    1f, 0f
        ))

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outCanvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        outCanvas.drawBitmap(bitmap, 0f, 0f, paint)

        return BitmapDrawable(null, output)
    }
}
