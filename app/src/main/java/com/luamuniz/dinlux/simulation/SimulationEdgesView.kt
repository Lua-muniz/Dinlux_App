package com.luamuniz.dinlux.simulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * View que desenha as arestas (linhas) entre os nós de um mesmo Grupo, um grafo
 * completo, sem nó central. Fica como camada de baixo, atrás dos círculos dos nós,
 * dentro do `content` do ZoomPanCanvasView
 */
class SimulationEdgesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Edge(
        val from: Pair<Int, Int>,
        val to: Pair<Int, Int>,
        val color: Int,
        val fromEntryId: String,
        val toEntryId: String
    )

    private var edges: List<Edge> = emptyList()

    // Id do nó sendo arrastado no momento as arestas que tocam esse
    // nó não são desenhadas enquanto ele estiver sendo arrastado, do mesmo jeito que o
    // próprio nó já some (ver SimulationCanvasRenderer.criarNodeView)
    private var idNoEscondido: String? = null

    private val paint = Paint().apply {
        strokeWidth = 4f
        alpha = 140
        isAntiAlias = true
    }

    fun setEdges(novasArestas: List<Edge>) {
        edges = novasArestas
        invalidate()
    }

    fun setNoEscondido(entryId: String?) {
        idNoEscondido = entryId
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        edges.forEach { edge ->
            if (edge.fromEntryId == idNoEscondido || edge.toEntryId == idNoEscondido) return@forEach
            paint.color = edge.color
            canvas.drawLine(
                edge.from.first.toFloat(), edge.from.second.toFloat(),
                edge.to.first.toFloat(), edge.to.second.toFloat(),
                paint
            )
        }
    }
}
