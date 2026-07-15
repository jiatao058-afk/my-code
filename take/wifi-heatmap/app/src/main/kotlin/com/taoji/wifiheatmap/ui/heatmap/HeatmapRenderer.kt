package com.taoji.wifiheatmap.ui.heatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.taoji.wifiheatmap.data.repo.PositionSummary

class HeatmapRenderer(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 36f
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 28f
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var positions: List<PositionSummary> = emptyList()
    private var touchedIndex: Int = -1

    fun setData(data: List<PositionSummary>) {
        positions = data.sortedBy { it.positionName }
        touchedIndex = -1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (positions.isEmpty()) {
            textPaint.color = Color.GRAY
            canvas.drawText("no data", width / 2f - 80f, height / 2f, textPaint)
            return
        }

        val barHeight = 80f
        val barSpacing = 24f
        val startY = 60f
        val maxBarWidth = width - 280f
        val startX = 220f

        textPaint.color = Color.BLACK
        canvas.drawText("Signal by Position (dBm)", 20f, 40f, textPaint)

        val minDBm = -100.0
        val maxDBm = -30.0

        var yPos = startY
        var idx = 0
        for (pos in positions) {
            val y = yPos
            yPos += barHeight + barSpacing
            val fraction = ((pos.avgLevel - minDBm) / (maxDBm - minDBm)).coerceIn(0.0, 1.0)
            val barW = (fraction * maxBarWidth).toInt()

            textPaint.color = Color.BLACK
            val displayName = if (pos.positionName.length > 8) pos.positionName.take(8) + ".." else pos.positionName
            canvas.drawText(displayName, 10f, y + barHeight / 2 + 10f, textPaint)

            barPaint.color = Color.LTGRAY
            barPaint.style = Paint.Style.FILL
            canvas.drawRect(startX, y, startX + maxBarWidth, y + barHeight, barPaint)

            barPaint.color = pos.color
            canvas.drawRect(startX, y, startX + barW, y + barHeight, barPaint)

            smallTextPaint.color = Color.BLACK
            val label = "${pos.avgLevel.toInt()} dBm (${pos.sampleCount})"
            canvas.drawText(label, startX + barW + 8f, y + barHeight / 2 + 8f, smallTextPaint)

            if (idx == touchedIndex) {
                barPaint.style = Paint.Style.STROKE
                barPaint.strokeWidth = 3f
                barPaint.color = Color.BLACK
                canvas.drawRect(startX - 2, y - 2, startX + maxBarWidth + 2, y + barHeight + 2, barPaint)
                barPaint.style = Paint.Style.FILL
            }
            idx++
        }

        val ly = height - 30f
        val legendColors = listOf(
            0xFF00E676.toInt() to "Good",
            0xFF76FF03.toInt() to "OK",
            0xFFFFEA00.toInt() to "Fair",
            0xFFFF9100.toInt() to "Weak",
            0xFFFF1744.toInt() to "Bad",
        )
        var lx = 20f
        for ((color, label) in legendColors) {
            legendPaint.color = color
            canvas.drawRect(lx, ly, lx + 20, ly + 20, legendPaint)
            smallTextPaint.color = Color.BLACK
            canvas.drawText(label, lx + 24, ly + 16, smallTextPaint)
            lx += smallTextPaint.measureText(label) + 36
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val barHeight = 80f
            val barSpacing = 24f
            val startY = 60f
            touchedIndex = -1
            var yPos = startY
            for (idx in positions.indices) {
                if (event.y >= yPos && event.y <= yPos + barHeight) {
                    touchedIndex = idx
                    break
                }
                yPos += barHeight + barSpacing
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }
}
