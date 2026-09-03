package com.luamuniz.dinlux.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.luamuniz.dinlux.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gráfico de linha desenhado sob medida em Canvas
 * Mostra, numa única linha ciano, o progresso de CADA SIMULAÇÃO
 * ATIVA como um todo, cada DashboardLineEntry é uma simulação, ligadas entre si por
 * uma linha. Diferente do DashboardBarChartView, esse é a visão "de longe": compara simulações inteiras entre si
 *
 * DashboardLineEntry.progress já chega pronto (0.0 a 1.0), a MÉDIA SIMPLES da fração
 * de conclusão de cada nó daquela simulação. Quem calcula é o GraphicsRepository; esta view só desenha
 */
class DashboardLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class DashboardLineEntry(val label: String, val progress: Double)

    var entries: List<DashboardLineEntry> = emptyList()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val paintTrilhaVazia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.dashboard_track_bg)
    }
    private val paintLinha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.cyan)
        strokeWidth = dpParaPx(2.5f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val paintPonto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = context.getColor(R.color.cyan)
    }
    private val paintRotulo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 217
        textSize = 22f
        textAlign = Paint.Align.LEFT
    }
    private val paintEixoPercentual = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 140
        textSize = 16f
        textAlign = Paint.Align.RIGHT
    }
    private val paintLinhaEixo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.white)
        alpha = 45
        strokeWidth = dpParaPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpParaPx(2.5f), dpParaPx(2.5f)), 0f)
    }

    private val alturaGrafico = dpParaPx(120f) // mesma altura da área de colunas do DashboardBarChartView
    private val alturaTrilhaVazia = dpParaPx(24f)
    private val raioPonto = dpParaPx(4f)
    private val espacoRotulo = dpParaPx(4f)
    private val espacoDepoisEixo = dpParaPx(6f)
    private val espacoTopoEixo = dpParaPx(14f)
    private val larguraMinimaPorPonto = dpParaPx(48f) // espaço mínimo garantido pro rótulo de cada ponto
    private val margemEsquerda = larguraMinimaPorPonto / 2f // afasta o 1º ponto da borda esquerda
    private val margemDireita = larguraMinimaPorPonto * 1.2f
    private val anguloRotuloGraus = 30f
    private val anguloRotuloRad = Math.toRadians(anguloRotuloGraus.toDouble())
    private val senoRotulo = sin(anguloRotuloRad).toFloat()
    private val cossenoRotulo = cos(anguloRotuloRad).toFloat()
    private val metricasRotulo = paintRotulo.fontMetrics
    private val espacoSegurancaAcimaAncora = -metricasRotulo.ascent * cossenoRotulo
    private val espacoAncoraAoTopo = espacoRotulo + espacoSegurancaAcimaAncora
    private val alturaMaximaRotulo = larguraMinimaPorPonto * senoRotulo + metricasRotulo.descent * cossenoRotulo
    private val larguraEixoPercentual = paintEixoPercentual.measureText("100%") + espacoDepoisEixo
    private fun dpParaPx(dp: Float): Float = dp * resources.displayMetrics.density

    // Largura mínima de conteúdo pra caber o eixo + todos os pontos com pelo menos
    // `larguraMinimaPorPonto` de espaço cada
    private fun calcularLarguraMinimaConteudo(): Float {
        if (entries.isEmpty()) return 0f
        val n = entries.size
        val espacoEntrePontos = if (n > 1) (n - 1) * larguraMinimaPorPonto else 0f
        return larguraEixoPercentual + margemEsquerda + margemDireita + espacoEntrePontos
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val disponivel = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val largura = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            disponivel
        } else {
            maxOf(disponivel, calcularLarguraMinimaConteudo())
        }
        setMeasuredDimension(largura.toInt(), calcularAltura().toInt())
    }

    private fun calcularAltura(): Float {
        if (entries.isEmpty()) return alturaTrilhaVazia
        return espacoTopoEixo + alturaGrafico + espacoAncoraAoTopo + alturaMaximaRotulo
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val largura = width.toFloat()

        if (entries.isEmpty()) {
            val trilha = RectF(0f, 0f, largura, alturaTrilhaVazia)
            canvas.drawRoundRect(trilha, alturaTrilhaVazia / 2f, alturaTrilhaVazia / 2f, paintTrilhaVazia)
            return
        }

        val topo = espacoTopoEixo
        val base = topo + alturaGrafico
        val n = entries.size
        val inicioPontos = larguraEixoPercentual + margemEsquerda
        val larguraParaPontos = (largura - larguraEixoPercentual - margemEsquerda - margemDireita).coerceAtLeast(0f)
        val centros = FloatArray(n) { index ->
            if (n == 1) {
                inicioPontos + larguraParaPontos / 2f
            } else {
                inicioPontos + larguraParaPontos * index / (n - 1).toFloat()
            }
        }

        // Eixo percentual (0% a 100%, de 10 em 10)
        for (nivel in 0..10) {
            val fracao = nivel / 10f
            val y = base - alturaGrafico * fracao
            canvas.drawLine(larguraEixoPercentual, y, largura, y, paintLinhaEixo)
            canvas.drawText(
                "${nivel * 10}%",
                larguraEixoPercentual - espacoDepoisEixo,
                y + paintEixoPercentual.textSize * 0.35f,
                paintEixoPercentual
            )
        }

        // Linha ciano ligando os pontos
        if (n > 1) {
            val path = Path()
            entries.forEachIndexed { index, entry ->
                val proporcao = entry.progress.coerceIn(0.0, 1.0)
                val y = base - alturaGrafico * proporcao.toFloat()
                if (index == 0) path.moveTo(centros[index], y) else path.lineTo(centros[index], y)
            }
            canvas.drawPath(path, paintLinha)
        }

        entries.forEachIndexed { index, entry ->
            val proporcao = entry.progress.coerceIn(0.0, 1.0)
            val y = base - alturaGrafico * proporcao.toFloat()
            canvas.drawCircle(centros[index], y, raioPonto, paintPonto)

            val rotuloTruncado = TextUtils.ellipsize(
                entry.label,
                paintRotulo,
                larguraMinimaPorPonto,
                TextUtils.TruncateAt.END
            )

            val ancoraX = centros[index]
            val ancoraY = base + espacoAncoraAoTopo
            canvas.save()
            canvas.rotate(anguloRotuloGraus, ancoraX, ancoraY)
            canvas.drawText(rotuloTruncado, 0, rotuloTruncado.length, ancoraX, ancoraY, paintRotulo)
            canvas.restore()
        }
    }
}
