package com.luamuniz.dinlux.graphics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.EntityColorPalette

/**
 * Um gráfico de banco/cartão dentro de uma DashboardSimulationSection: título (ex:
 * "Mercado Pago Débito", ou nulo pro gráfico fantasma de uma simulação sem lançamento
 * nenhum ainda) + a lista de colunas (uma por lançamento daquele banco/cartão)
 *
 * colorSourceId é o id (do cartão, ou do banco se for débito/economia) usado pra resolver
 * a cor via EntityColorPalette, resolvida só na hora de desenhar (`SectionViewHolder`),
 * já que o GraphicsRepository que monta esses dados não tem `Context`
 */
data class DashboardEntityChart(
    val label: String?,
    val colorSourceId: String,
    val entries: List<DashboardBarChartView.DashboardBarEntry>
)

data class DashboardSimulationSection(
    val simulationTitle: String?,
    val entityCharts: List<DashboardEntityChart>,
    val progressoGeral: Double = 0.0
)

class DashboardSectionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var sections: List<DashboardSimulationSection> = listOf(secaoVazia())
    private var progressoPorSimulacao: List<DashboardLineChartView.DashboardLineEntry> = emptyList()
    private var indicesCor: Map<String, Int> = emptyMap()
    private fun secaoVazia() = DashboardSimulationSection(
        simulationTitle = null,
        entityCharts = listOf(DashboardEntityChart(label = null, colorSourceId = "", entries = emptyList()))
    )

    fun atualizar(novasSecoes: List<DashboardSimulationSection>) {
        progressoPorSimulacao = novasSecoes.map {
            DashboardLineChartView.DashboardLineEntry(label = it.simulationTitle.orEmpty(), progress = it.progressoGeral)
        }
        sections = novasSecoes.ifEmpty { listOf(secaoVazia()) }
        val todosOsIds = sections.flatMap { secao -> secao.entityCharts.map { it.colorSourceId } }
        indicesCor = EntityColorPalette.resolveIndices(todosOsIds)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) TYPE_HEADER else TYPE_SECTION

    override fun getItemCount(): Int = sections.size + 1 // +1 pelo cabeçalho "Progresso Geral"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_dashboard_progress_header, parent, false))
        } else {
            SectionViewHolder(inflater.inflate(R.layout.item_dashboard_simulation_section, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(progressoPorSimulacao)
            is SectionViewHolder -> holder.bind(sections[position - 1], indicesCor)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chart: DashboardLineChartView = itemView.findViewById(R.id.chart_dashboard_progresso_geral)

        fun bind(entries: List<DashboardLineChartView.DashboardLineEntry>) {
            chart.entries = entries
        }
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTitle: TextView = itemView.findViewById(R.id.text_dashboard_section_title)
        private val containerCharts: LinearLayout = itemView.findViewById(R.id.container_dashboard_section_charts)

        fun bind(section: DashboardSimulationSection, indicesCor: Map<String, Int>) {
            if (section.simulationTitle.isNullOrBlank()) {
                textTitle.visibility = View.GONE
            } else {
                textTitle.visibility = View.VISIBLE
                textTitle.text = section.simulationTitle
            }

            containerCharts.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)
            section.entityCharts.forEach { entidade ->
                val cardView = inflater.inflate(R.layout.item_dashboard_chart, containerCharts, false)
                val rowTitle: LinearLayout = cardView.findViewById(R.id.row_dashboard_chart_title)
                val textEntityTitle: TextView = cardView.findViewById(R.id.text_dashboard_chart_title)
                val dotColor: View = cardView.findViewById(R.id.dot_dashboard_chart_color)
                val chartView: DashboardBarChartView = cardView.findViewById(R.id.chart_dashboard)

                if (entidade.label.isNullOrBlank()) {
                    rowTitle.visibility = View.GONE
                    chartView.entries = entidade.entries
                } else {
                    rowTitle.visibility = View.VISIBLE
                    textEntityTitle.text = entidade.label
                    val indice = indicesCor[entidade.colorSourceId]
                    val cor = if (indice != null) {
                        EntityColorPalette.colorAt(itemView.context, indice)
                    } else {
                        EntityColorPalette.colorFor(itemView.context, entidade.colorSourceId)
                    }
                    dotColor.backgroundTintList = android.content.res.ColorStateList.valueOf(cor)
                    chartView.entries = entidade.entries.map { it.copy(color = cor) }
                }

                containerCharts.addView(cardView)
            }
        }
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SECTION = 1
    }
}
