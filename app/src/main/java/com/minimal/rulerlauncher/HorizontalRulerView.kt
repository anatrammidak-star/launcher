package com.minimal.rulerlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class HorizontalRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabet = ('A'..'Z').toList()
    private var scrollOffset = 0f
    private var selectedIndex = 0
    private var lastTouchX = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    var onLetterSelectedListener: ((Char) -> Unit)? = null

    init {
        // Default awal di huruf J atau A
        selectedIndex = 9 // Huruf 'J' di tengah
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val itemWidth = w / 7f // Menampilkan sekitar 7 huruf dalam satu layar (sepertiga rentang)

        val centerX = w / 2f

        for (i in alphabet.indices) {
            // Posisi X relatif terhadap gesekan scroll
            val x = centerX + (i - selectedIndex) * itemWidth + scrollOffset
            
            // Batasi render hanya huruf yang mendekati layar agar ringan
            if (x < -itemWidth || x > w + itemWidth) continue

            val distanceFromCenter = abs(x - centerX)
            val isSelected = distanceFromCenter < (itemWidth / 2f)

            // Garis skala milimeter di atas huruf
            val tickLen = if (isSelected) 18f else 10f
            tickPaint.strokeWidth = if (isSelected) 2f else 1f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#444444")
            canvas.drawLine(x, 4f, x, 4f + tickLen, tickPaint)

            // Sub-garis milimeter kecil di antara huruf
            tickPaint.strokeWidth = 1f
            tickPaint.color = Color.parseColor("#262626")
            canvas.drawLine(x + (itemWidth / 2f), 4f, x + (itemWidth / 2f), 4f + 6f, tickPaint)

            // Render huruf A-Z
            val alpha = (255f * (1f - (distanceFromCenter / (w / 2f)).coerceIn(0f, 0.8f))).toInt()
            textPaint.color = if (isSelected) Color.WHITE else Color.argb(alpha.coerceAtLeast(50), 200, 200, 200)
            textPaint.textSize = if (isSelected) 38f else 26f
            textPaint.isFakeBoldText = isSelected

            canvas.drawText(alphabet[i].toString(), x, h - 12f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                scrollOffset += dx
                lastTouchX = event.x

                val itemWidth = width.toFloat() / 7f
                if (abs(scrollOffset) > itemWidth) {
                    val steps = (scrollOffset / itemWidth).toInt()
                    selectedIndex = (selectedIndex - steps).coerceIn(0, alphabet.size - 1)
                    scrollOffset %= itemWidth
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onLetterSelectedListener?.invoke(alphabet[selectedIndex])
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scrollOffset = 0f
                invalidate()
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
            onLetterSelectedListener?.invoke(alphabet[selectedIndex])
        }
    }
}
