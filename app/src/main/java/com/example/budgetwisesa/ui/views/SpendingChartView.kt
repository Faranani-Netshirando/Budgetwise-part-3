package com.example.budgetwisesa.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.budgetwisesa.R
import com.google.android.material.color.MaterialColors
import kotlin.math.min

/** Native Canvas chart; the financial data and accessible category legend stay in ordinary Views. */
class SpendingChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var values = emptyList<Pair<String, Double>>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun update(values: List<Pair<String, Double>>) { this.values = values; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = values.sumOf { it.second }
        if (total <= 0) return
        val diameter = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom) * 0.80f
        val x = width / 2f
        val y = height / 2f
        val stroke = diameter * 0.16f
        val bounds = RectF(x - diameter / 2, y - diameter / 2, x + diameter / 2, y + diameter / 2)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        var angle = -90f
        values.forEachIndexed { index, entry ->
            val sweep = (entry.second / total * 360).toFloat()
            paint.color = colors[index % colors.size]
            canvas.drawArc(bounds, angle, sweep, false, paint)
            angle += sweep
        }
        paint.style = Paint.Style.FILL
        paint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13 * resources.displayMetrics.scaledDensity
        canvas.drawText(context.getString(R.string.total_spent), x, y - 10, paint)
        paint.textSize = 22 * resources.displayMetrics.scaledDensity
        val label = money(total)
        while (paint.measureText(label) > diameter * 0.7f && paint.textSize > 10) paint.textSize -= 1
        canvas.drawText(label, x, y + paint.textSize, paint)
    }
    companion object {
        val colors = intArrayOf(Color.rgb(19, 117, 71), Color.rgb(23, 103, 181),
            Color.rgb(191, 69, 69), Color.rgb(141, 102, 0), Color.rgb(135, 83, 165))
    }
}
