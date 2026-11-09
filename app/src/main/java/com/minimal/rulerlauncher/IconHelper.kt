package com.minimal.rulerlauncher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.util.Locale

object IconHelper {

    /**
     * Menghasilkan ikon minimalis dengan Smart ID (2-3 huruf) 
     * dan garis outline squircle beresolusi tajam.
     */
    fun createMinimalistIcon(rawDrawable: Drawable, label: String): Drawable {
        val totalSize = 120
        val outputBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // 1. Ekstrak warna khas dari ikon asli untuk garis luar (Outline)
        val outlineColor = extractDominantColor(rawDrawable)

        val strokeWidth = 4.5f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = outlineColor
        }

        val padding = strokeWidth + 6f
        val rect = RectF(padding, padding, totalSize - padding, totalSize - padding)
        val cornerRadius = 26f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // 2. Buat singkatan unik 2-3 huruf (Smart ID) dari nama aplikasi
        val smartId = generateSmartId(label)

        // 3. Render teks singkatan di tengah bingkai squircle
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (smartId.length > 2) 34f else 42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(smartId, canvas.width / 2f, textY, textPaint)

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Membuat singkatan 2-3 huruf yang mudah dikenali secara universal untuk semua aplikasi
     */
    private fun generateSmartId(name: String): String {
        val clean = name.trim()
        if (clean.isEmpty()) return "?"
        
        val words = clean.split(Regex("\\s+"))
        return if (words.size > 1) {
            val initials = words.take(2).map { it.first().uppercaseChar() }.joinToString("")
            initials
        } else {
            if (clean.length >= 3) {
                clean.substring(0, 3).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            } else {
                clean.uppercase(Locale.ROOT)
            }
        }
    }

    /**
     * Mengekstrak warna dominan dari ikon asli untuk garis luar squircle
     */
    private fun extractDominantColor(drawable: Drawable): Int {
        val size = 32
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        var redSum = 0
        var greenSum = 0
        var blueSum = 0
        var count = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 50) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    
                    if (!(r > 230 && g > 230 && b > 230) && !(r < 25 && g < 25 && b < 25)) {
                        redSum += r
                        greenSum += g
                        blueSum += b
                        count++
                    }
                }
            }
        }

        return if (count > 0) {
            Color.rgb(redSum / count, greenSum / count, blueSum / count)
        } else {
            Color.parseColor("#E0E0E0")
        }
    }
}
