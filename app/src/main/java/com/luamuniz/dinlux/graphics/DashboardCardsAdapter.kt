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
import java.text.NumberFormat
import java.util.Locale

/**
 * Um card de doughnut da aba "Cartões" um por cartão de crédito cadastrado (nome do cartão, nome do banco, limite usado/disponível, dia de
 * fechamento/vencimento). colorSourceId é o id do CARTÃO, mesma cor que ele já tem no
 * menu "Cartões cadastrados"/canvas de simulação/dashboard de Simulações, resolvida só na
 * hora de desenhar (EntityColorPalette), já que o GraphicsRepository que monta esses
 * dados não tem `Context`
 */
data class DashboardCardDonut(
    val cardLabel: String?,
    val bankName: String,
    val colorSourceId: String,
    val usado: Double,
    val disponivel: Double,
    val closingDay: Int,
    val dueDay: Int
)

class DashboardCardsAdapter : RecyclerView.Adapter<DashboardCardsAdapter.ViewHolder>() {

    private var cards: List<DashboardCardDonut> = listOf(cardVazio())

    // Índice da cor de cada colorSourceId, sem colisão/parecença entre os cards dessa
    // mesma lista (mesmo padrão do resto do dashboard)
    private var indicesCor: Map<String, Int> = emptyMap()

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private fun cardVazio() = DashboardCardDonut(
        cardLabel = null,
        bankName = "",
        colorSourceId = "",
        usado = 0.0,
        disponivel = 0.0,
        closingDay = 0,
        dueDay = 0
    )

    fun atualizar(novosCards: List<DashboardCardDonut>) {
        cards = novosCards.ifEmpty { listOf(cardVazio()) }
        indicesCor = EntityColorPalette.resolveIndices(cards.map { it.colorSourceId })
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowTitle: LinearLayout = view.findViewById(R.id.row_dashboard_card_title)
        val textLabel: TextView = view.findViewById(R.id.text_dashboard_card_label)
        val textBank: TextView = view.findViewById(R.id.text_dashboard_card_bank)
        val dotColor: View = view.findViewById(R.id.dot_dashboard_card_color)
        val chart: DashboardCardDonutChartView = view.findViewById(R.id.chart_dashboard_card_donut)
        val textClosing: TextView = view.findViewById(R.id.text_dashboard_card_closing)
        val textDue: TextView = view.findViewById(R.id.text_dashboard_card_due)
        val rowUsado: LinearLayout = view.findViewById(R.id.row_dashboard_card_usado)
        val rowDisponivel: LinearLayout = view.findViewById(R.id.row_dashboard_card_disponivel)
        val textUsado: TextView = view.findViewById(R.id.text_dashboard_card_usado)
        val textDisponivel: TextView = view.findViewById(R.id.text_dashboard_card_disponivel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_card_donut, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = cards[position]
        val temCartao = !card.cardLabel.isNullOrBlank()

        holder.rowTitle.visibility = if (temCartao) View.VISIBLE else View.GONE
        holder.textClosing.visibility = if (temCartao) View.VISIBLE else View.GONE
        holder.textDue.visibility = if (temCartao) View.VISIBLE else View.GONE
        holder.rowUsado.visibility = if (temCartao) View.VISIBLE else View.GONE
        holder.rowDisponivel.visibility = if (temCartao) View.VISIBLE else View.GONE

        if (temCartao) {
            holder.textLabel.text = card.cardLabel
            holder.textBank.text = card.bankName
            holder.textClosing.text = "Fechamento: dia ${card.closingDay}"
            holder.textDue.text = "Vencimento: dia ${card.dueDay}"

            val indice = indicesCor[card.colorSourceId]
            val cor = if (indice != null) {
                EntityColorPalette.colorAt(holder.itemView.context, indice)
            } else {
                EntityColorPalette.colorFor(holder.itemView.context, card.colorSourceId)
            }
            holder.dotColor.backgroundTintList = ColorStateList.valueOf(cor)

            // Porcentagem de cada fatia pra legenda
            val total = card.usado + card.disponivel
            val percentualUsado = if (total > 0.0) Math.round(card.usado / total * 100.0).toInt() else 0
            val percentualDisponivel = if (total > 0.0) 100 - percentualUsado else 100
            holder.textUsado.text = "Usado: ${currencyFormat.format(card.usado)} ($percentualUsado%)"
            holder.textDisponivel.text = "Disponível: ${currencyFormat.format(card.disponivel)} ($percentualDisponivel%)"
        }

        holder.chart.usado = card.usado
        holder.chart.disponivel = card.disponivel
    }

    override fun getItemCount(): Int = cards.size
}
