package com.luamuniz.dinlux.excerpt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Uma linha de extrato pronta pra exibição em tabela (Data | Valor | Descrição). Modelo
 * comum entre o resumo de Importar Extrato (StatementTransaction, ainda não salvo) e o
 * extrato exibido em Finanças (StatementTransactionRecord, já salvo). Cada tela converte
 * seu próprio tipo pra este antes de passar pro ExtratoAdapter (ver
 * `ImportExtratoActivity.exibirResumo` e `Finance.exibirExtrato`).
 */
data class ExtratoLinha(
    val date: Long,
    val description: String,
    val amount: Double,
    val credit: Boolean
)

class ExtratoAdapter : RecyclerView.Adapter<ExtratoAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var linhas: List<ExtratoLinha> = emptyList()

    fun atualizar(novasLinhas: List<ExtratoLinha>) {
        linhas = novasLinhas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_extrato_linha, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(linhas[position])
    }

    override fun getItemCount(): Int = linhas.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textData: TextView = view.findViewById(R.id.text_coluna_data)
        private val textValor: TextView = view.findViewById(R.id.text_coluna_valor)
        private val textDescricao: TextView = view.findViewById(R.id.text_coluna_descricao)

        fun bind(linha: ExtratoLinha) {
            textData.text = dateFormat.format(Date(linha.date))

            val sinal = if (linha.credit) "+" else "-"
            textValor.text = "$sinal ${currencyFormat.format(abs(linha.amount))}"
            textValor.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (linha.credit) R.color.extrato_entrada else R.color.extrato_saida
                )
            )

            textDescricao.text = linha.description
        }
    }
}
