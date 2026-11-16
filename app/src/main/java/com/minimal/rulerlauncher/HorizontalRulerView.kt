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

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    var onLetterSelectedListener: ((Char) -> Unit)? = null

    init {
        selectedIndex = 0 // Default awal di huruf 'A'
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val itemWidth = w / 7f // Menampilkan 7 huruf dalam satu layar (sepertiga rentang)
        val centerX = w / 2f

        // --- 1. Gambar 3 Titik Pintasan Cepat (Quick Jump Dots) di Bagian Atas ---
        val dotY = 14f
        val dotSpacing = 36f
        val startDotX = centerX - dotSpacing
        for (d in 0..2) {
            val cx = startDotX + (d * dotSpacing)
            val isActiveSegment = when (d) {
                0 -> selectedIndex in 0..8       // Kelompok A - I
                1 -> selectedIndex in 9..17      // Kelompok J - R
                else -> selectedIndex in 18..25  // Kelompok S - Z
            }
            dotPaint.color = if (isActiveSegment) Color.WHITE else Color.parseColor("#4D4D4D")
            canvas.drawCircle(cx, dotY, if (isActiveSegment) 6.5f else 4.5f, dotPaint)
        }

        // --- 2. Render Penggaris Abjad Melingkar Penuh (True Visual Loop: Z -> A -> B) ---
        val tickTopY = 34f
        // Render 9 slot di sekitar indeks tengah (-4 sampai +4) agar selalu penuh ke tepi layar
        for (offset in -4..4) {
            val wrappedIndex = ((selectedIndex + offset) % alphabet.size + alphabet.size) % alphabet.size
            val x = centerX + (offset * itemWidth) + scrollOffset

            if (x < -itemWidth || x > w + itemWidth) continue

            val distanceFromCenter = abs(x - centerX)
            val isSelected = distanceFromCenter < (itemWidth / 2f)

            // Garis skala milimeter utama tepat di atas huruf
            val mainTickLen = if (isSelected) 24f else 14f
            tickPaint.strokeWidth = if (isSelected) 2.5f else 1.2f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#666666")
            canvas.drawLine(x, tickTopY, x, tickTopY + mainTickLen, tickPaint)

            // 3 Sub-garis skala milimeter halus di antara huruf (seperti penggaris asli)
            val subTickLen = 8f
            tickPaint.strokeWidth = 1f
            tickPaint.color = Color.parseColor("#383838")
            for (sub in 1..3) {
                val subX = x + (sub * itemWidth / 4f)
                canvas.drawLine(subX, tickTopY, subX, tickTopY + subTickLen, tickPaint)
            }

            // Render huruf A-Z dengan gradasi memudar ke tepi layar
            val fadeRatio = (1f - (distanceFromCenter / (w * 0.52f))).coerceIn(0f, 1f)
            val alpha = (255f * fadeRatio).toInt().coerceAtLeast(45)
            textPaint.color = if (isSelected) Color.WHITE else Color.argb(alpha, 210, 210, 210)
            textPaint.textSize = if (isSelected) 46f else 28f
            textPaint.isFakeBoldText = isSelected

            canvas.drawText(alphabet[wrappedIndex].toString(), x, h - 10f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                // Deteksi ketukan pada area Quick Jump Dots (42px teratas)
                if (event.y <= 42f) {
                    val segmentWidth = width.toFloat() / 3f
                    val targetIndex = when {
                        event.x < segmentWidth -> 0          // Lompat ke 'A' (Kelompok A-I)
                        event.x < segmentWidth * 2 -> 12     // Lompat ke 'M' (Kelompok J-R)
                        else -> 19                           // Lompat ke 'T' (Kelompok S-Z)
                    }
                    selectedIndex = targetIndex
                    scrollOffset = 0f
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLetterSelectedListener?.invoke(alphabet[selectedIndex])
                    invalidate()
                    return true
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                scrollOffset += dx
                lastTouchX = event.x

                val itemWidth = width.toFloat() / 7f
                if (abs(scrollOffset) >= itemWidth) {
                    val steps = (scrollOffset / itemWidth).toInt()
                    selectedIndex = ((selectedIndex - steps) % alphabet.size + alphabet.size) % alphabet.size
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
            scrollOffset = 0f
            invalidate()
            onLetterSelectedListener?.invoke(alphabet[selectedIndex])
        }
    }
}
