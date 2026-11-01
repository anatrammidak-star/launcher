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
    private var selectedIndex = 0

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        color = Color.parseColor("#333333")
    }

    var onLetterSelectedListener: ((Char) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val step = h / alphabet.size

        for (i in alphabet.indices) {
            val y = (i * step) + (step / 2)
            val isSelected = (i == selectedIndex)

            // Gambar garis skala penggaris di sisi kiri huruf
            val tickLength = if (isSelected) 24f else 10f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#383838")
            canvas.drawLine(8f, y, 8f + tickLength, y, tickPaint)

            // Gambar huruf A-Z
            textPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#666666")
            textPaint.textSize = if (isSelected) 36f else 24f
            textPaint.isFakeBoldText = isSelected
            canvas.drawText(alphabet[i].toString(), 36f, y + 8f, textPaint)
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
}
