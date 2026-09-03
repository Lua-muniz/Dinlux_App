package com.luamuniz.dinlux.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.luamuniz.dinlux.R

/**
 * Gráfico de COLUNAS verticais desenhado sob medida em Canvas (sem biblioteca externa,
 * mesmo espírito do canvas de simulação, ZoomPanCanvasView/SimulationEdgesView) pro
 * dashboard embutido na Home
 *
 * Cada DashboardBarEntry vira uma coluna: trilha cinza representando 0%–100% de altura,
 * preenchimento colorido subindo de baixo pra cima representando DashboardBarEntry.value
 * sobre DashboardBarEntry.max, com o rótulo do lançamento embaixo (truncado com "…" se
 * não couber na largura da coluna). Quando DashboardBarEntry.groupLabel muda de uma
 * coluna pra outra, o nome do grupo aparece centralizado acima
 * do conjunto de colunas daquele grupo, numa única fileira no topo, como um eixo de
 * categoria de dois níveis
 *
 * Se a largura de todas as colunas juntas não couber na largura da view, elas encolhem
 * proporcionalmente (mantendo os espaçamentos relativos) — o card do dashboard tem largura
 * fixa (260dp) e não tem scroll interno próprio, então nada pode vazar pra fora
 */
class DashboardBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class DashboardBarEntry(
        val label: String,
        val value: Double,
        val max: Double,
        val color: Int,
        val groupLabel: String? = null,
        val contornoDestacado: Boolean = false
    )

    var entries: List<DashboardBarEntry> = emptyList()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val corTrilha = context.getColor(R.color.dashboard_track_bg)

    private val paintTrilha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = corTrilha
    }
    private val paintPreenchimento = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    // Contorno verde fino pra distinguir economia de compra quando as duas usam a mesma
    // cor (a do banco/cartão), ver nota do `contornoDestacado` na doc da classe
    private val paintContornoDestacado = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.aviso_badge_verde)
        strokeWidth = dpParaPx(1.5f)
    }
    // TextPaint (não só Paint) porque TextUtils.ellipsize() exige TextPaint, usado pra
    // truncar o rótulo do lançamento quando não cabe na largura da coluna
    private val paintRotulo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 217 // ~85%, rótulo discreto — o destaque é a coluna colorida, não o texto
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    private val paintGrupo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 140 // mais discreto ainda que o rótulo da coluna — é só contexto extra
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }
    // Marcações do eixo de porcentagem, texto menor que o rótulo/grupo e alinhado à direita, terminando
    // logo antes da área do gráfico
    private val paintEixoPercentual = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        alpha = 140
        textSize = 16f
        textAlign = Paint.Align.RIGHT
    }
    // Linha pontilhada clara atrás das colunas
    private val paintLinhaEixo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.white)
        alpha = 45
        strokeWidth = dpParaPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(dpParaPx(2.5f), dpParaPx(2.5f)), 0f)
    }

    private val alturaMaximaColuna = dpParaPx(120f) // ver KDoc: aumentada de 90dp pra caber as 11 marcações do eixo
    private val alturaTrilhaVazia = dpParaPx(24f) // altura do estado vazio
    private val larguraColunaPadrao = dpParaPx(28f)
    private val espacoEntreColunas = dpParaPx(10f)
    private val espacoEntreGrupos = dpParaPx(18f)
    private val espacoRotuloColuna = dpParaPx(4f)
    private val espacoDepoisGrupo = dpParaPx(6f)
    private val raioCantoColuna = dpParaPx(4f)
    private val espacoDepoisEixo = dpParaPx(6f) // gap entre o texto "100%" e o início da área do gráfico
    private val espacoTopoEixo = dpParaPx(14f) // espaço acima do topo das colunas pro texto "100%" não cortar
    private val larguraEixoPercentual = paintEixoPercentual.measureText("100%") + espacoDepoisEixo
    private fun dpParaPx(dp: Float): Float = dp * resources.displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), calcularAltura().toInt())
    }

    private fun temGrupo(): Boolean = entries.any { it.groupLabel != null }

    // Não depende da largura (só a altura muda entre estado vazio/cheio). Garante que a
    // altura medida bate exatamente com o que é desenhado, mesmo com o rótulo de grupo
    private fun calcularAltura(): Float {
        if (entries.isEmpty()) return alturaTrilhaVazia

        val alturaLinhaGrupo = if (temGrupo()) paintGrupo.textSize + espacoDepoisGrupo else 0f
        val alturaLinhaRotulo = espacoRotuloColuna + paintRotulo.textSize
        return alturaLinhaGrupo + espacoTopoEixo + alturaMaximaColuna + alturaLinhaRotulo
    }

    // Posição (centro) e largura de cada coluna, encolhendo todo mundo proporcionalmente se
    // a soma natural (largura padrão de cada coluna + espaçamentos) não couber na largura
    // disponível, evita vazar pra fora do card (260dp, sem scroll interno próprio)
    private data class LayoutColuna(val centroX: Float, val largura: Float)

    private fun calcularLayoutColunas(larguraDisponivel: Float): List<LayoutColuna> {
        if (entries.isEmpty()) return emptyList()

        val gaps = FloatArray(entries.size) // gaps[i] = espaço antes da coluna i (0 pra i=0)
        for (i in entries.indices) {
            gaps[i] = when {
                i == 0 -> 0f
                entries[i].groupLabel != null && entries[i].groupLabel != entries[i - 1].groupLabel -> espacoEntreGrupos
                else -> espacoEntreColunas
            }
        }

        val larguraNatural = entries.size * larguraColunaPadrao + gaps.sum()
        val escala = if (larguraNatural > larguraDisponivel && larguraNatural > 0f) {
            larguraDisponivel / larguraNatural
        } else {
            1f
        }

        val larguraColuna = larguraColunaPadrao * escala
        val layout = mutableListOf<LayoutColuna>()
        var x = 0f
        entries.indices.forEach { index ->
            x += gaps[index] * escala
            layout.add(LayoutColuna(centroX = x + larguraColuna / 2f, largura = larguraColuna))
            x += larguraColuna
        }
        return layout
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val largura = width.toFloat()

        if (entries.isEmpty()) {
            val trilha = RectF(0f, 0f, largura, alturaTrilhaVazia)
            canvas.drawRoundRect(trilha, alturaTrilhaVazia / 2f, alturaTrilhaVazia / 2f, paintTrilha)
            return
        }

        val comGrupo = temGrupo()
        val alturaLinhaGrupo = if (comGrupo) paintGrupo.textSize + espacoDepoisGrupo else 0f
        val topoColunas = alturaLinhaGrupo + espacoTopoEixo
        val baseColunas = topoColunas + alturaMaximaColuna

        // Colunas calculadas na largura que sobra depois de reservar o eixo percentual à
        // esquerda, depois deslocadas pra direita por essa mesma largura
        val larguraParaColunas = (largura - larguraEixoPercentual).coerceAtLeast(0f)
        val layout = calcularLayoutColunas(larguraParaColunas).map {
            it.copy(centroX = it.centroX + larguraEixoPercentual)
        }

        // Rótulos de grupo, uma única fileira no topo, um texto centralizado por trecho
        // contíguo de colunas com o mesmo groupLabel (eixo de categoria de dois níveis)
        if (comGrupo) {
            var inicio = 0
            while (inicio < entries.size) {
                val grupoAtual = entries[inicio].groupLabel
                var fim = inicio
                while (fim + 1 < entries.size && entries[fim + 1].groupLabel == grupoAtual) {
                    fim++
                }
                if (grupoAtual != null) {
                    val bordaEsquerda = layout[inicio].centroX - layout[inicio].largura / 2f
                    val bordaDireita = layout[fim].centroX + layout[fim].largura / 2f
                    val centroGrupo = (bordaEsquerda + bordaDireita) / 2f
                    canvas.drawText(grupoAtual, centroGrupo, paintGrupo.textSize - 2f, paintGrupo)
                }
                inicio = fim + 1
            }
        }

        entries.forEachIndexed { index, _ ->
            val col = layout[index]
            val esquerda = col.centroX - col.largura / 2f
            val direita = col.centroX + col.largura / 2f
            val trilha = RectF(esquerda, topoColunas, direita, baseColunas)
            canvas.drawRoundRect(trilha, raioCantoColuna, raioCantoColuna, paintTrilha)
        }

        for (nivel in 0..10) {
            val fracao = nivel / 10f
            val y = baseColunas - alturaMaximaColuna * fracao
            canvas.drawLine(larguraEixoPercentual, y, largura, y, paintLinhaEixo)
            canvas.drawText(
                "${nivel * 10}%",
                larguraEixoPercentual - espacoDepoisEixo,
                y + paintEixoPercentual.textSize * 0.35f,
                paintEixoPercentual
            )
        }

        // Preenchimento colorido (progresso real) + contorno + rótulo do nó —
        // desenhados por cima da linha do eixo, escondendo ela onde já tem progresso
        entries.forEachIndexed { index, entry ->
            val col = layout[index]
            val esquerda = col.centroX - col.largura / 2f
            val direita = col.centroX + col.largura / 2f
            val trilha = RectF(esquerda, topoColunas, direita, baseColunas)

            val proporcao = if (entry.max > 0) (entry.value / entry.max).coerceIn(0.0, 1.0) else 0.0
            if (proporcao > 0.0) {
                paintPreenchimento.color = entry.color
                val alturaPreenchida = (alturaMaximaColuna * proporcao).toFloat()
                val preenchido = RectF(esquerda, baseColunas - alturaPreenchida, direita, baseColunas)
                canvas.drawRoundRect(preenchido, raioCantoColuna, raioCantoColuna, paintPreenchimento)
            }

            if (entry.contornoDestacado) {
                canvas.drawRoundRect(trilha, raioCantoColuna, raioCantoColuna, paintContornoDestacado)
            }

            val larguraMaximaTexto = col.largura + espacoEntreColunas
            val rotuloTruncado = TextUtils.ellipsize(
                entry.label,
                paintRotulo,
                larguraMaximaTexto,
                TextUtils.TruncateAt.END
            )
            canvas.drawText(
                rotuloTruncado, 0, rotuloTruncado.length,
                col.centroX, baseColunas + espacoRotuloColuna + paintRotulo.textSize - 4f,
                paintRotulo
            )
        }
    }
}
