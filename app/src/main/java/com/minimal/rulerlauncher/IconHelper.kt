package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object IconHelper {

    /**
     * Membuat ikon outline minimalis bergaris padat dengan inisial huruf di tengah.
     * Tidak membutuhkan file drawable per aplikasi dan menghasilkan gaya yang seragam.
     */
    fun createMinimalistIcon(label: String): Drawable {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Bingkai garis luar (squircle) padat putih bersih
        val strokeWidth = 4.5f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = Color.parseColor("#E0E0E0")
        }

        val padding = strokeWidth + 6f
        val rect = RectF(padding, padding, size - padding, size - padding)
        val cornerRadius = 26f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // Karakter inisial huruf di tengah
        val letter = if (label.isNotBlank()) label.trim().first().uppercaseChar().toString() else "?"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        // Penyesuaian titik vertikal teks agar tepat berada di pusat kanvas
        val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(letter, canvas.width / 2f, textY, textPaint)

        return BitmapDrawable(null, bitmap)
    }
}
