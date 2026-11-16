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
import kotlin.math.hypot

class HorizontalRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val alphabet = ('A'..'Z').toList()
    private var scrollOffset = 0f
    private var selectedIndex = 0
    private var lastTouchX = 0f

    // Simpan koordinat pusat 3 titik agar area sentuh akurat dan tidak bocor ke area lain
    private val dotCentersX = FloatArray(3)
    private var dotCenterY = 0f
    private val dotHitRadius = 48f

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
        val itemWidth = w / 7f
        val centerX = w / 2f

        // --- Tata Letak Vertikal dari Bawah ke Atas ---
        // 1. Huruf A-Z mepet ke bagian paling bawah
        val textBaselineY = h - 10f

        // 2. Skala milimeter diturunkan agar dekat di atas huruf A-Z
        val tickBottomY = textBaselineY - 42f
        val mainTickMaxLen = 22f
        val tickTopY = tickBottomY - mainTickMaxLen

        // 3. 3 Titik Quick Jump diturunkan, namun tetap berjarak jelas di atas skala ruler
        dotCenterY = tickTopY - 34f
        val dotSpacing = 68f
        val startDotX = centerX - dotSpacing

        for (d in 0..2) {
            val cx = startDotX + (d * dotSpacing)
            dotCentersX[d] = cx

            val isActiveSegment = when (d) {
                0 -> selectedIndex in 0..8       // Kelompok A - I
                1 -> selectedIndex in 9..17      // Kelompok J - R
                else -> selectedIndex in 18..25  // Kelompok S - Z
            }
            dotPaint.color = if (isActiveSegment) Color.WHITE else Color.parseColor("#555555")
            canvas.drawCircle(cx, dotCenterY, if (isActiveSegment) 8.5f else 6f, dotPaint)
        }

        // --- Render Penggaris Abjad Melingkar Penuh ---
        for (offset in -4..4) {
            val wrappedIndex = ((selectedIndex + offset) % alphabet.size + alphabet.size) % alphabet.size
            val x = centerX + (offset * itemWidth) + scrollOffset

            if (x < -itemWidth || x > w + itemWidth) continue

            val distanceFromCenter = abs(x - centerX)
            val isSelected = distanceFromCenter < (itemWidth / 2f)

            // Garis skala milimeter utama (tumbuh ke atas dari tickBottomY agar selalu dekat ke huruf)
            val mainTickLen = if (isSelected) mainTickMaxLen else 13f
            tickPaint.strokeWidth = if (isSelected) 2.5f else 1.2f
            tickPaint.color = if (isSelected) Color.WHITE else Color.parseColor("#666666")
            canvas.drawLine(x, tickBottomY - mainTickLen, x, tickBottomY, tickPaint)

            // 3 Sub-garis skala milimeter halus di antara huruf
            val subTickLen = 7f
            tickPaint.strokeWidth = 1f
            tickPaint.color = Color.parseColor("#383838")
            for (sub in 1..3) {
                val subX = x + (sub * itemWidth / 4f)
                canvas.drawLine(subX, tickBottomY - subTickLen, subX, tickBottomY, tickPaint)
            }

            // Render huruf A-Z
            val fadeRatio = (1f - (distanceFromCenter / (w * 0.52f))).coerceIn(0f, 1f)
            val alpha = (255f * fadeRatio).toInt().coerceAtLeast(45)
            textPaint.color = if (isSelected) Color.WHITE else Color.argb(alpha, 210, 210, 210)
            textPaint.textSize = if (isSelected) 46f else 28f
            textPaint.isFakeBoldText = isSelected

            canvas.drawText(alphabet[wrappedIndex].toString(), x, textBaselineY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x

                // Cek apakah sentuhan tepat berada di radius salah satu dari 3 titik Quick Jump
                for (d in 0..2) {
                    val distanceToDot = hypot(event.x - dotCentersX[d], event.y - dotCenterY)
                    if (distanceToDot <= dotHitRadius) {
                        val targetIndex = when (d) {
                            0 -> 0   // Lompat ke 'A' (Kelompok A-I)
                            1 -> 12  // Lompat ke 'M' (Kelompok J-R)
                            else -> 19 // Lompat ke 'T' (Kelompok S-Z)
                        }
                        selectedIndex = targetIndex
                        scrollOffset = 0f
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onLetterSelectedListener?.invoke(alphabet[selectedIndex])
                        invalidate()
                        return true
                    }
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
