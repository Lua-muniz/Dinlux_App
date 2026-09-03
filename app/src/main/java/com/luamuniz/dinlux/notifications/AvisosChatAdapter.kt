package com.luamuniz.dinlux.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.simulation.SimulationEntryType
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Lista o histórico de mensagens do chat de um banco, agrupado em sessões, uma por nó
 * (compra ou economia)
 */
class AvisosChatAdapter(
    private val onConfirmar: (AvisoSecao, AvisoMensagem) -> Unit,
    private val onSolicitarDesconfirmar: (AvisoSecao, AvisoMensagem) -> Unit,
    private val onOptionsClick: (AvisoSecao) -> Unit,
    private val onAvisoCartaoClick: (AvisoSecao) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val secao: AvisoSecao, val colapsada: Boolean) : Row()
        data class Item(val secao: AvisoSecao, val mensagem: AvisoMensagem) : Row()
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    private var rows: List<Row> = emptyList()
    private var ultimasSecoes: List<AvisoSecao> = emptyList()
    private val travadas = mutableSetOf<String>()
    private val secoesColapsadas = mutableSetOf<String>()
    private val secoesJaVistas = mutableSetOf<String>()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM/yyyy", Locale("pt", "BR"))

    fun atualizarLista(novasSecoes: List<AvisoSecao>) {
        novasSecoes.forEach { secao ->
            if (secoesJaVistas.add(secao.entry.id)) {
                secoesColapsadas.add(secao.entry.id)
            }
        }
        ultimasSecoes = novasSecoes
        reconstruirRows()
    }

    private fun reconstruirRows() {
        val novasRows = mutableListOf<Row>()
        ultimasSecoes.forEach { secao ->
            val colapsada = secao.entry.id in secoesColapsadas
            novasRows.add(Row.Header(secao, colapsada))
            if (!colapsada) {
                secao.mensagens.forEach { mensagem -> novasRows.add(Row.Item(secao, mensagem)) }
            }
        }
        rows = novasRows
        notifyDataSetChanged()
    }

    fun alternarColapso(entryId: String) {
        if (!secoesColapsadas.remove(entryId)) {
            secoesColapsadas.add(entryId)
        }
        reconstruirRows()
    }

    private fun chaveMensagem(secao: AvisoSecao, mensagem: AvisoMensagem): String =
        "${secao.entry.id}:${mensagem.periodo}"

    // Acha a posição exata dessa mensagem na lista de rows atual usado pra atualizar só essa linha
    private fun indiceDe(secao: AvisoSecao, mensagem: AvisoMensagem): Int {
        return rows.indexOfFirst { row ->
            row is Row.Item && row.secao.entry.id == secao.entry.id && row.mensagem.periodo == mensagem.periodo
        }
    }

    fun marcarEmProgresso(secao: AvisoSecao, mensagem: AvisoMensagem) {
        travadas.add(chaveMensagem(secao, mensagem))
        val indice = indiceDe(secao, mensagem)
        if (indice >= 0) notifyItemChanged(indice)
    }

    fun desfazerProgresso(secao: AvisoSecao, mensagem: AvisoMensagem) {
        travadas.remove(chaveMensagem(secao, mensagem))
        val indice = indiceDe(secao, mensagem)
        if (indice >= 0) notifyItemChanged(indice)
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> VIEW_TYPE_HEADER
        is Row.Item -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aviso_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aviso_pergunta, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).bind(row.secao, row.colapsada)
            is Row.Item -> (holder as ItemViewHolder).bind(row.secao, row.mensagem)
        }
    }

    override fun getItemCount(): Int = rows.size

    private fun formatarMes(periodo: YearMonth): String {
        val texto = periodo.format(monthFormat)
        return texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val indicadorNovidade = view.findViewById<View>(R.id.indicador_secao_novidade)
        private val textTitulo = view.findViewById<TextView>(R.id.text_secao_titulo)
        private val textSubtitulo = view.findViewById<TextView>(R.id.text_secao_subtitulo)
        private val buttonOptions = view.findViewById<ImageButton>(R.id.button_secao_options)
        private val buttonColapsar = view.findViewById<ImageButton>(R.id.button_secao_colapsar)
        private val buttonAvisoCartao = view.findViewById<ImageButton>(R.id.button_secao_aviso_cartao)

        fun bind(secao: AvisoSecao, colapsada: Boolean) {
            textTitulo.text = secao.titulo
            textSubtitulo.text = secao.subtitulo

            indicadorNovidade.visibility = if (secao.naoVistasCount > 0) View.VISIBLE else View.INVISIBLE

            if (secao.finalizada) {
                buttonOptions.visibility = View.VISIBLE
                buttonOptions.setOnClickListener { onOptionsClick(secao) }
            } else {
                buttonOptions.visibility = View.INVISIBLE
                buttonOptions.setOnClickListener(null)
            }

            buttonColapsar.rotation = if (colapsada) 180f else 0f
            buttonColapsar.contentDescription = itemView.context.getString(
                if (colapsada) R.string.content_description_aviso_section_expand
                else R.string.content_description_aviso_section_collapse
            )
            buttonColapsar.setOnClickListener { alternarColapso(secao.entry.id) }

            if (secao.cardFechamentoAlterado) {
                buttonAvisoCartao.visibility = View.VISIBLE
                buttonAvisoCartao.setOnClickListener { onAvisoCartaoClick(secao) }
            } else {
                buttonAvisoCartao.visibility = View.GONE
                buttonAvisoCartao.setOnClickListener(null)
            }
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textData = view.findViewById<TextView>(R.id.text_aviso_data)
        private val textPergunta = view.findViewById<TextView>(R.id.text_aviso_pergunta)
        private val textDetalhe = view.findViewById<TextView>(R.id.text_aviso_detalhe)
        private val checkbox = view.findViewById<CheckBox>(R.id.checkbox_aviso_pergunta)

        fun bind(secao: AvisoSecao, mensagem: AvisoMensagem) {
            textData.text = formatarMes(mensagem.periodo)

            when (secao.tipo) {
                SimulationEntryType.SAVINGS -> {
                    textPergunta.text = "A economia \"${secao.titulo}\" no valor de " +
                        "${currencyFormat.format(secao.entry.monthlyAmount)} foi guardada?"
                }
                SimulationEntryType.PURCHASE -> {
                    textPergunta.text = "A parcela de \"${secao.titulo}\" (${secao.subtitulo}) no valor de " +
                        "${currencyFormat.format(secao.entry.installmentValue)} foi paga?"
                }
            }

            val atrasada = !mensagem.paga && mensagem.periodo.isBefore(YearMonth.now())
            textDetalhe.text = when {
                mensagem.paga -> "Confirmada"
                atrasada -> "Atrasada"
                else -> "Pendente"
            }
            // "Confirmada" em verde
            textDetalhe.setTextColor(
                textDetalhe.context.getColor(
                    when {
                        mensagem.paga -> R.color.aviso_badge_verde
                        atrasada -> R.color.alert_red
                        else -> R.color.white
                    }
                )
            )

            aplicarEstadoCheckbox(secao, mensagem)
        }

        // Reaplica o estado real (paga/não paga) no checkbox, travado ou não
        private fun aplicarEstadoCheckbox(secao: AvisoSecao, mensagem: AvisoMensagem) {
            val travada = chaveMensagem(secao, mensagem) in travadas
            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = mensagem.paga
            checkbox.isEnabled = !travada
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                when {
                    travada -> Unit
                    isChecked && !mensagem.paga -> onConfirmar(secao, mensagem)
                    !isChecked && mensagem.paga -> {
                        // Desmarcar desfaz um desconto/economia real
                        aplicarEstadoCheckbox(secao, mensagem)
                        onSolicitarDesconfirmar(secao, mensagem)
                    }
                    else -> Unit
                }
            }
        }
    }
}
