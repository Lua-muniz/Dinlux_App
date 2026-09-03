package com.luamuniz.dinlux.graphics

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.EntityColorPalette

/**
 * Um card de pizza da aba Movimentações: nome do banco (nulo pro card fantasma, sem banco
 * cadastrado nenhum) + total de entrada/saída somado de TODO o extrato já importado pra
 * esse banco (todas as importações, deduplicadas, ver `StatementTransactionRepository`)
 */
data class DashboardBankPieCard(
    val bankLabel: String?,
    val colorSourceId: String,
    val entrada: Double,
    val saida: Double
)

/**
 * Lista VERTICAL de pizzas, uma por banco cadastrado, aqui é uma lista simples um card por banco
 *
 * `atualizar` nunca deixa a lista vazia de verdade (nenhum banco cadastrado) mostra um
 * único card fantasma sem título e com a pizza zerada/cinza, que o
 * DashboardPieChartView já sabe desenhar sozinho
 */
class DashboardMovimentacoesAdapter : RecyclerView.Adapter<DashboardMovimentacoesAdapter.ViewHolder>() {

    private var cards: List<DashboardBankPieCard> = listOf(cardVazio())
    private var indicesCor: Map<String, Int> = emptyMap()
    private fun cardVazio() = DashboardBankPieCard(
        bankLabel = null,
        colorSourceId = "",
        entrada = 0.0,
        saida = 0.0
    )

    fun atualizar(novosCards: List<DashboardBankPieCard>) {
        cards = novosCards.ifEmpty { listOf(cardVazio()) }
        indicesCor = EntityColorPalette.resolveIndices(cards.map { it.colorSourceId })
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowTitle: LinearLayout = view.findViewById(R.id.row_dashboard_pie_title)
        val textTitle: TextView = view.findViewById(R.id.text_dashboard_pie_title)
        val dotColor: View = view.findViewById(R.id.dot_dashboard_pie_color)
        val chart: DashboardPieChartView = view.findViewById(R.id.chart_dashboard_pie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_pie_chart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = cards[position]
        if (card.bankLabel.isNullOrBlank()) {
            holder.rowTitle.visibility = View.GONE
        } else {
            holder.rowTitle.visibility = View.VISIBLE
            holder.textTitle.text = card.bankLabel
            val indice = indicesCor[card.colorSourceId]
            val cor = if (indice != null) {
                EntityColorPalette.colorAt(holder.itemView.context, indice)
            } else {
                EntityColorPalette.colorFor(holder.itemView.context, card.colorSourceId)
            }
            holder.dotColor.backgroundTintList = ColorStateList.valueOf(cor)
        }
        holder.chart.entrada = card.entrada
        holder.chart.saida = card.saida
    }

    override fun getItemCount(): Int = cards.size
}
