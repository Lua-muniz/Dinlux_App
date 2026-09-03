package com.luamuniz.dinlux.simulation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

/**
 * Lista de simulações separada em duas seções, Ativas e Inativas, cada uma com um
 * cabeçalho simples de texto. Uma seção só aparece se tiver pelo menos uma simulação
 */
class SimulationListAdapter(
    private val onClick: (Simulation) -> Unit,
    private val onOptionsClick: (Simulation) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class Header(val title: String) : Row()
        data class Item(val simulation: Simulation) : Row()
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    private var rows: List<Row> = emptyList()

    fun atualizarLista(novosItens: List<Simulation>) {
        val ativas = novosItens.filter { it.active }
        val inativas = novosItens.filter { !it.active }

        val novasRows = mutableListOf<Row>()
        if (ativas.isNotEmpty()) {
            novasRows.add(Row.Header("Ativas"))
            ativas.forEach { novasRows.add(Row.Item(it)) }
        }
        if (inativas.isNotEmpty()) {
            novasRows.add(Row.Header("Inativas"))
            inativas.forEach { novasRows.add(Row.Item(it)) }
        }
        rows = novasRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is Row.Header -> VIEW_TYPE_HEADER
            is Row.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simulation_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_simulation, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderViewHolder).bind(row.title)
            is Row.Item -> (holder as ItemViewHolder).bind(row.simulation)
        }
    }

    override fun getItemCount(): Int = rows.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textHeader = view.findViewById<TextView>(R.id.text_simulation_section_header)

        fun bind(title: String) {
            textHeader.text = title
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textTitle = view.findViewById<TextView>(R.id.text_simulation_title)
        private val buttonOptions = view.findViewById<ImageButton>(R.id.button_simulation_options)

        fun bind(simulation: Simulation) {
            textTitle.text = simulation.title
            itemView.setOnClickListener { onClick(simulation) }
            buttonOptions.setOnClickListener { onOptionsClick(simulation) }
        }
    }
}
