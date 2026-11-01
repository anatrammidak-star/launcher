package com.minimal.rulerlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class RulerSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabet = ('A'..'Z').toList()
    private var selectedIndex = 25 // Default Z sesuai gambar acuan

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    var onLetterSelectedListener: ((Char) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val step = h / alphabet.size

        for (i in alphabet.indices) {
            val y = (i * step) + (step / 2f)
            val isSelected = (i == selectedIndex)

            // Garis skala utama tepat di samping huruf
            val mainTickLen = if (isSelected) 22f else 12f
            tickPaint.strokeWidth = if (isSelected) 2.5f else 1.5f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#444444")
            canvas.drawLine(10f, y, 10f + mainTickLen, y, tickPaint)

            // Sub-garis milimeter pendek di antara huruf (gaya penggaris presisi)
            if (i < alphabet.size - 1) {
                val subY = y + (step / 2f)
                tickPaint.strokeWidth = 1f
                tickPaint.color = Color.parseColor("#262626")
                canvas.drawLine(10f, subY, 10f + 6f, subY, tickPaint)
            }

            // Teks huruf A-Z
            textPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#666666")
            textPaint.textSize = if (isSelected) 36f else 24f
            textPaint.isFakeBoldText = isSelected
            canvas.drawText(alphabet[i].toString(), 42f, y + 8f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val step = height.toFloat() / alphabet.size
                val rawIndex = (event.y / step).toInt().coerceIn(0, alphabet.size - 1)

                if (rawIndex != selectedIndex) {
                    selectedIndex = rawIndex
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onLetterSelectedListener?.invoke(alphabet[selectedIndex])
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setSelectedLetter(letter: Char) {
        val idx = alphabet.indexOf(letter.uppercaseChar())
        if (idx != -1 && idx != selectedIndex) {
            selectedIndex = idx
            invalidate()
        }
    }
}
