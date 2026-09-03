package com.luamuniz.dinlux.simulation

import android.content.ClipData
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.EntityColorPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Desenha os Grupos (grafos completos de até SimulationGroup.MAX_NODES_PER_GROUP nós)
 * dentro do `content` do ZoomPanCanvasView. Cada Grupo vira um cluster: os nós ficam
 * em círculo ao redor de um centro (o centro é só referência do layout, não é um nó de
 * verdade), e uma aresta conecta cada par de nós do mesmo Grupo. Clusters diferentes
 * ficam numa grade, um por Grupo. Com espaço suficiente pra nunca dois nós de
 * clusters diferentes se tocarem
 */
class SimulationCanvasRenderer(private val context: Context) {

    companion object {
        private const val COLUMNS = 3

        // Nó: cresce com o nome até MAX, depois passa a quebrar em 2 linhas
        private const val MIN_NODE_SIZE_DP = 56
        private const val MAX_NODE_SIZE_DP = 104
        private const val NODE_TEXT_SIZE_SP = 11f
        private const val NODE_TEXT_SIZE_SP_TWO_LINES = 9.5f
        private const val NODE_HORIZONTAL_PADDING_DP = 12

        // Espaçamento: nunca deixa nós (do mesmo Grupo ou de Grupos vizinhos) se tocarem
        private const val NODE_ORBIT_BASE_DP = 70 // raio mínimo do círculo de nós dentro do cluster
        private const val NODE_GAP_DP = 16        // folga mínima entre bordas de nós vizinhos
        private const val CLUSTER_GAP_DP = 40      // folga mínima entre clusters vizinhos
        private const val BASE_CLUSTER_SPACING_DP = 220
        private const val GROUP_LABEL_RESERVE_DP = 40 // espaço reservado acima do cluster pro nome

        // "sombra" (drag shadow) do nó arrastado, ampliada em relação ao tamanho real do nó
        private const val DRAG_SHADOW_SCALE = 1.8f
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    private fun spToPx(sp: Float): Float = sp * context.resources.displayMetrics.scaledDensity

    private data class NodeLayoutInfo(val diameterPx: Int, val duasLinhas: Boolean)

    private fun calcularLayoutNo(nome: String): NodeLayoutInfo {
        val paint = Paint().apply { textSize = spToPx(NODE_TEXT_SIZE_SP) }
        val larguraTexto = paint.measureText(nome)
        val padding = dp(NODE_HORIZONTAL_PADDING_DP * 2)
        val necessarioUmaLinha = (larguraTexto + padding).toInt()
        val tamanhoMaximo = dp(MAX_NODE_SIZE_DP)

        return if (necessarioUmaLinha <= tamanhoMaximo) {
            NodeLayoutInfo(necessarioUmaLinha.coerceAtLeast(dp(MIN_NODE_SIZE_DP)), duasLinhas = false)
        } else {
            NodeLayoutInfo(tamanhoMaximo, duasLinhas = true)
        }
    }

    private fun isCompativel(entry: SimulationEntry, grupo: SimulationGroup): Boolean {
        return grupo.id != entry.groupId &&
            grupo.bankId == entry.bankId &&
            grupo.cardId == entry.cardId &&
            grupo.entryType == entry.type &&
            grupo.nodeCount < SimulationGroup.MAX_NODES_PER_GROUP
    }

    fun render(
        content: FrameLayout,
        groups: List<SimulationGroup>,
        entries: List<SimulationEntry>,
        onNodeClick: (SimulationEntry) -> Unit,
        onNodeMoved: (SimulationEntry, SimulationGroup) -> Unit,
        onGroupClick: (SimulationGroup) -> Unit
    ) {
        content.removeAllViews()
        if (groups.isEmpty()) return

        val entriesByGroup = entries.groupBy { it.groupId }

        // Cor de cada Grupo resolvida de uma vez só, pra todos os Grupos deste canvas
        val indicesCor = EntityColorPalette.resolveIndices(
            groups.map { it.cardId.ifEmpty { it.bankId } }
        )

        // Pra cada Grupo, calcula o raio da "órbita" dos nós e o raio total do
        // cluster (órbita + metade do maior nó), sem ainda posicionar nada, precisamos
        // do maior raio de cluster entre todos pra decidir o espaçamento da grade
        data class GrupoCalculado(
            val grupo: SimulationGroup,
            val entradas: List<SimulationEntry>,
            val layouts: Map<String, NodeLayoutInfo>,
            val orbitaRadius: Int,
            val clusterRadius: Int
        )

        val calculados = groups.map { grupo ->
            val entradas = entriesByGroup[grupo.id].orEmpty()
            val layouts = entradas.associate { it.id to calcularLayoutNo(it.title) }
            val maiorDiametro = layouts.values.maxOfOrNull { it.diameterPx } ?: dp(MIN_NODE_SIZE_DP)
            val orbitaRadius = if (entradas.size <= 1) 0 else maxOf(dp(NODE_ORBIT_BASE_DP), maiorDiametro + dp(NODE_GAP_DP))
            val clusterRadius = orbitaRadius + maiorDiametro / 2
            GrupoCalculado(grupo, entradas, layouts, orbitaRadius, clusterRadius)
        }

        val maiorClusterRadius = calculados.maxOfOrNull { it.clusterRadius } ?: dp(NODE_ORBIT_BASE_DP)
        val spacing = maxOf(
            dp(BASE_CLUSTER_SPACING_DP),
            2 * maiorClusterRadius + dp(CLUSTER_GAP_DP) + dp(GROUP_LABEL_RESERVE_DP)
        )

        val rows = (groups.size + COLUMNS - 1) / COLUMNS
        val contentWidth = spacing * COLUMNS
        val contentHeight = spacing * rows
        content.layoutParams = FrameLayout.LayoutParams(contentWidth, contentHeight)

        content.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENDED -> {
                    if (!event.result) {
                        Toast.makeText(
                            context,
                            "Não foi possível mover: solte sobre um Grupo compatível com vaga.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                else -> true
            }
        }

        val edgesView = SimulationEdgesView(context)
        content.addView(edgesView, FrameLayout.LayoutParams(contentWidth, contentHeight))

        val allEdges = mutableListOf<SimulationEdgesView.Edge>()

        calculados.forEachIndexed { index, calculado ->
            val col = index % COLUMNS
            val row = index / COLUMNS
            val centerX = col * spacing + spacing / 2
            val centerY = row * spacing + spacing / 2
            val grupo = calculado.grupo
            val entradas = calculado.entradas

            // Área de soltar (drag and drop) cobre todo o cluster, inclusive a margem
            // de segurança, mas fica invisível (só aparece um destaque ao arrastar por cima)
            val dropZoneSize = 2 * calculado.clusterRadius
            val dropZone = View(context).apply {
                background = null
            }
            content.addView(
                dropZone,
                FrameLayout.LayoutParams(dropZoneSize, dropZoneSize).apply {
                    leftMargin = centerX - dropZoneSize / 2
                    topMargin = centerY - dropZoneSize / 2
                }
            )
            dropZone.setOnDragListener { view, event ->
                val entryArrastado = event.localState as? SimulationEntry
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> entryArrastado != null
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        if (entryArrastado != null && isCompativel(entryArrastado, grupo)) {
                            view.setBackgroundColor(Color.argb(70, 255, 255, 255))
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        view.setBackgroundColor(Color.TRANSPARENT)
                        if (entryArrastado != null && isCompativel(entryArrastado, grupo)) {
                            onNodeMoved(entryArrastado, grupo)
                            true
                        } else {
                            false
                        }
                    }
                    else -> true
                }
            }

            val chaveCor = grupo.cardId.ifEmpty { grupo.bankId }
            val indiceCor = indicesCor[chaveCor]
            val color = if (indiceCor != null) {
                EntityColorPalette.colorAt(context, indiceCor)
            } else {
                EntityColorPalette.colorFor(context, chaveCor)
            }
            // Guarda o id do nó junto com o centro dele (nodeCenters), é o que permite
            // montar as arestas (mais abaixo) já sabendo a quais dois nós cada uma
            // pertence, pra poder esconder quando um desses nós está sendo arrastado
            data class CentroNo(val x: Int, val y: Int, val entryId: String)

            val nodeCenters = mutableListOf<CentroNo>()
            val angleStep = if (entradas.isEmpty()) 0.0 else 2 * Math.PI / entradas.size

            entradas.forEachIndexed { nodeIndex, entry ->
                val angle = -Math.PI / 2 + angleStep * nodeIndex // começa no topo, sentido horário
                val nodeX = centerX + (calculado.orbitaRadius * cos(angle)).toInt()
                val nodeY = centerY + (calculado.orbitaRadius * sin(angle)).toInt()
                nodeCenters.add(CentroNo(nodeX, nodeY, entry.id))

                val layoutInfo = calculado.layouts.getValue(entry.id)
                val nodeView = criarNodeView(entry, color, layoutInfo, onNodeClick, edgesView)
                val params = FrameLayout.LayoutParams(layoutInfo.diameterPx, layoutInfo.diameterPx)
                params.leftMargin = nodeX - layoutInfo.diameterPx / 2
                params.topMargin = nodeY - layoutInfo.diameterPx / 2
                content.addView(nodeView, params)
            }

            // Grafo completo: liga cada par de nós do Grupo
            for (i in nodeCenters.indices) {
                for (j in i + 1 until nodeCenters.size) {
                    val de = nodeCenters[i]
                    val para = nodeCenters[j]
                    allEdges.add(
                        SimulationEdgesView.Edge(
                            de.x to de.y, para.x to para.y, color, de.entryId, para.entryId
                        )
                    )
                }
            }

            // Legenda do Grupo (nome), com um fundo pra ficar legível em cima do canvas
            // escuro — tocável, abre renomear (onGroupClick)
            val groupLabel = TextView(context).apply {
                text = grupo.name
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(dp(10), dp(4), dp(10), dp(4))
                background = GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(Color.argb(160, 0, 0, 0))
                }
                setOnClickListener { onGroupClick(grupo) }
            }
            val labelParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            labelParams.leftMargin = centerX - dp(GROUP_LABEL_RESERVE_DP)
            labelParams.topMargin = (centerY - calculado.clusterRadius - dp(GROUP_LABEL_RESERVE_DP)).coerceAtLeast(0)
            content.addView(groupLabel, labelParams)
        }

        edgesView.setEdges(allEdges)
    }

    private fun criarNodeView(
        entry: SimulationEntry,
        color: Int,
        layoutInfo: NodeLayoutInfo,
        onNodeClick: (SimulationEntry) -> Unit,
        edgesView: SimulationEdgesView
    ): FrameLayout {
        val diameter = layoutInfo.diameterPx

        val container = FrameLayout(context).apply {
            tag = entry.id
            isLongClickable = true
            setOnClickListener { onNodeClick(entry) }
            setOnLongClickListener { view ->
                val clipData = ClipData.newPlainText("simulationEntryId", entry.id)
                val shadow = ScaledDragShadowBuilder(view, DRAG_SHADOW_SCALE)
                view.startDragAndDrop(clipData, shadow, entry, 0)
            }
            // Enquanto o nó está sendo arrastado, ele some do lugar original na tela
            setOnDragListener { view, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val souEsteNo = event.localState === entry
                        if (souEsteNo) {
                            view.visibility = View.INVISIBLE
                            edgesView.setNoEscondido(entry.id)
                        }
                        souEsteNo
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        if (event.localState === entry) {
                            view.visibility = View.VISIBLE
                            edgesView.setNoEscondido(null)
                        }
                        true
                    }
                    else -> true
                }
            }
        }

        val circle = ImageView(context).apply {
            setBackgroundResource(R.drawable.shape_circle)
            backgroundTintList = ColorStateList.valueOf(color)
        }
        container.addView(circle, FrameLayout.LayoutParams(diameter, diameter))

        val label = TextView(context).apply {
            text = entry.title
            setTextColor(Color.WHITE)
            textSize = if (layoutInfo.duasLinhas) NODE_TEXT_SIZE_SP_TWO_LINES else NODE_TEXT_SIZE_SP
            maxLines = if (layoutInfo.duasLinhas) 2 else 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            setPadding(dp(NODE_HORIZONTAL_PADDING_DP), 0, dp(NODE_HORIZONTAL_PADDING_DP), 0)
        }
        container.addView(
            label,
            FrameLayout.LayoutParams(diameter, diameter, Gravity.CENTER)
        )

        return container
    }
    private class ScaledDragShadowBuilder(view: View, private val scale: Float) : View.DragShadowBuilder(view) {
        override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
            val width = (view.width * scale).toInt()
            val height = (view.height * scale).toInt()
            outShadowSize.set(width, height)
            outShadowTouchPoint.set(width / 2, height / 2)
        }

        override fun onDrawShadow(canvas: Canvas) {
            canvas.save()
            canvas.scale(scale, scale)
            view.draw(canvas)
            canvas.restore()
        }
    }
}
