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
     * Menghasilkan ikon minimalis berkelas ala Niagara/Minimal Launcher:
     * Bingkai squircle outline tipis berwawasan warna dinamis dengan Smart ID (2-3 huruf) yang elegan.
     */
    fun createMinimalistIcon(rawDrawable: Drawable, label: String): Drawable {
        val totalSize = 120
        val outputBitmap = Bitmap.createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // 1. Ekstrak warna dominan dari ikon asli untuk garis luar (Dynamic Outline Color)
        val outlineColor = extractDominantColor(rawDrawable)

        // 2. Gambar bingkai squircle outline halus dan tipis
        val strokeWidth = 3.5f
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = outlineColor
        }

        val padding = strokeWidth + 6f
        val rect = RectF(padding, padding, totalSize - padding, totalSize - padding)
        val cornerRadius = 28f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

        // 3. Buat singkatan unik 2-3 huruf (Smart ID) dari nama aplikasi
        val smartId = generateSmartId(label)

        // 4. Render teks singkatan tipografi modern di tengah bingkai
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (smartId.length > 2) 32f else 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        val textY = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(smartId, canvas.width / 2f, textY, textPaint)

        return BitmapDrawable(null, outputBitmap)
    }

    /**
     * Membuat singkatan 2-3 huruf yang mudah dikenali (Contoh: Camera -> Cam, Canva -> Can, ChatGPT -> CG)
     */
    private fun generateSmartId(name: String): String {
        val clean = name.trim()
        if (clean.isEmpty()) return "?"
        
        val words = clean.split(Regex("\\s+"))
        return if (words.size > 1) {
            words.take(2).map { it.first().uppercaseChar() }.joinToString("")
        } else {
            if (clean.length >= 3) {
                clean.substring(0, 3).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            } else {
                clean.uppercase(Locale.ROOT)
            }
        }
    }

    /**
     * Mengekstrak warna dominan dari ikon asli agar garis luar squircle memiliki warna khas yang elegan
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

Presenter:
        for (x in 0 until size) {
            for (y in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha > 40) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    
                    // Hindari warna hitam/putih murni agar warna outline tetap hidup
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
            Color.parseColor("#CCCCCC") // Warna cadangan abu-abu terang netral
        }
    }
}
