package com.luamuniz.dinlux.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.luamuniz.dinlux.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Gráfico de pizza desenhado sob medida em Canvas pra aba Movimentações: duas
 * fatias, verde (entrada) e vermelha (saída)
 *
 * Quando entrada e saida são as duas zero (banco sem nenhum extrato importado ainda),
 * a view desenha uma pizza inteira cinza (fantasma) em vez de ficar em branco
 */
class DashboardPieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var entrada: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }
    var saida: Double = 0.0
        set(value) {
            field = value
            invalidate()
        }

    private val paintFatiaEntrada = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.extrato_entrada)
    }
    private val paintFatiaSaida = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.extrato_saida)
    }
    private val paintFatiaVazia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.dashboard_track_bg)
    }
    private val paintLegenda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 217
        textSize = 24f
    }

    private val diametroPizza = dpParaPx(120f)
    private val raioDot = dpParaPx(5f)
    private val espacoDotTexto = dpParaPx(8f)
    private val alturaLinhaLegenda = dpParaPx(22f)
    private val espacoPizzaLegenda = dpParaPx(14f)
    private val espacoEntreLinhasLegenda = dpParaPx(6f)

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private fun dpParaPx(dp: Float): Float = dp * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val altura = diametroPizza + espacoPizzaLegenda +
            alturaLinhaLegenda + espacoEntreLinhasLegenda + alturaLinhaLegenda
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), altura.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val largura = width.toFloat()
        val centroX = largura / 2f
        val pizzaRect = RectF(centroX - diametroPizza / 2f, 0f, centroX + diametroPizza / 2f, diametroPizza)

        val total = entrada + saida
        if (total <= 0.0) {
            canvas.drawOval(pizzaRect, paintFatiaVazia)
        } else {
            val anguloEntrada = (entrada / total * 360.0).toFloat()
            canvas.drawArc(pizzaRect, -90f, anguloEntrada, true, paintFatiaEntrada)
            canvas.drawArc(pizzaRect, -90f + anguloEntrada, 360f - anguloEntrada, true, paintFatiaSaida)
        }

        var y = diametroPizza + espacoPizzaLegenda + alturaLinhaLegenda - dpParaPx(6f)
        desenharLinhaLegenda(canvas, y, paintFatiaEntrada, "Entrada: ${formatoMoeda.format(entrada)}")
        y += alturaLinhaLegenda + espacoEntreLinhasLegenda
        desenharLinhaLegenda(canvas, y, paintFatiaSaida, "Saída: ${formatoMoeda.format(saida)}")
    }

    private fun desenharLinhaLegenda(canvas: Canvas, y: Float, paintCor: Paint, texto: String) {
        canvas.drawCircle(raioDot, y - raioDot / 2f, raioDot, paintCor)
        canvas.drawText(texto, raioDot * 2 + espacoDotTexto, y, paintLegenda)
    }
}
