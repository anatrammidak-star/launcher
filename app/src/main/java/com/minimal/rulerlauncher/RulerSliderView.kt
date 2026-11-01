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
        textSize = 40f
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
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

            // Garis skala penggaris
            val tickLength = if (isSelected) 28f else 12f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#444444")
            canvas.drawLine(6f, y, 6f + tickLength, y, tickPaint)

            // Huruf A-Z dibesarkan 1.7x
            textPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#777777")
            textPaint.textSize = if (isSelected) 60f else 40f
            textPaint.isFakeBoldText = isSelected
            canvas.drawText(alphabet[i].toString(), 46f, y + 14f, textPaint)
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
