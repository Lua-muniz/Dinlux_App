package com.luamuniz.dinlux.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.luamuniz.dinlux.R

/**
 * Gráfico de doughnut desenhado sob medida em Canvas, pra aba "Cartões" do dashboard.
 * Duas fatias: vermelho (usado, limite já consumido do cartão) e verde (disponivel) reaproveita as mesmas
 * cores já usadas na tabela de extrato (`extrato_saida`/`extrato_entrada`)
 */
class DashboardCardDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var usado: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }
    var disponivel: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    private val paintFatiaUsado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.extrato_saida)
        strokeCap = Paint.Cap.BUTT
    }
    private val paintFatiaDisponivel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.extrato_entrada)
        strokeCap = Paint.Cap.BUTT
    }

    private val diametroDonut = dpParaPx(90f)
    private val espessuraAnel = dpParaPx(16f)

    init {
        paintFatiaUsado.strokeWidth = espessuraAnel
        paintFatiaDisponivel.strokeWidth = espessuraAnel
    }

    private fun dpParaPx(dp: Float): Float = dp * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(diametroDonut.toInt(), diametroDonut.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val raio = (diametroDonut - espessuraAnel) / 2f
        val centro = diametroDonut / 2f
        val anelRect = RectF(centro - raio, centro - raio, centro + raio, centro + raio)

        val total = usado + disponivel
        if (total <= 0.0) {
            canvas.drawArc(anelRect, 0f, 360f, false, paintFatiaDisponivel)
            return
        }

        val anguloUsado = (usado / total * 360.0).toFloat()
        if (anguloUsado > 0f) {
            canvas.drawArc(anelRect, -90f, anguloUsado, false, paintFatiaUsado)
        }
        if (anguloUsado < 360f) {
            canvas.drawArc(anelRect, -90f + anguloUsado, 360f - anguloUsado, false, paintFatiaDisponivel)
        }
    }
}
