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
        selectedIndex = 0 // Default awal di huruf 'A'
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val itemWidth = w / 7f // Menampilkan sekitar 7 huruf dalam satu layar

        val centerX = w / 2f

        for (i in alphabet.indices) {
            val x = centerX + (i - selectedIndex) * itemWidth + scrollOffset
            
            if (x < -itemWidth || x > w + itemWidth) continue

            val distanceFromCenter = abs(x - centerX)
            val isSelected = distanceFromCenter < (itemWidth / 2f)

            // 1. Skala Garis Milimeter di atas Huruf A-Z
            // Garis tick utama tepat di atas huruf
            val mainTickLen = if (isSelected) 22f else 14f
            tickPaint.strokeWidth = if (isSelected) 2.5f else 1.2f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#666666")
            canvas.drawLine(x, 2f, x, 2f + mainTickLen, tickPaint)

            // Sub-garis tick milimeter kecil di antara huruf (pembagi skala)
            val subTickLen = if (isSelected) 12f else 8f
            tickPaint.strokeWidth = 1f
            tickPaint.color = Color.parseColor("#444444")
            canvas.drawLine(x + (itemWidth / 3f), 2f, x + (itemWidth / 3f), 2f + subTickLen, tickPaint)
            canvas.drawLine(x + (2f * itemWidth / 3f), 2f, x + (2f * itemWidth / 3f), 2f + subTickLen, tickPaint)

            // 2. Render Huruf A-Z
            val alpha = (255f * (1f - (distanceFromCenter / (w / 2f)).coerceIn(0f, 0.8f))).toInt()
            textPaint.color = if (isSelected) Color.WHITE else Color.argb(alpha.coerceAtLeast(60), 200, 200, 200)
            textPaint.textSize = if (isSelected) 40f else 28f
            textPaint.isFakeBoldText = isSelected

            // Posisi teks huruf berada di bawah skala garis milimeter
            canvas.drawText(alphabet[i].toString(), x, h - 8f, textPaint)
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
