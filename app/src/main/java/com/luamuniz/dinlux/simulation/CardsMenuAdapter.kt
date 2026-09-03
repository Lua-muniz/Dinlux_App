package com.luamuniz.dinlux.simulation

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.EntityColorPalette

/** Uma linha do menu de cartões: id do banco (débito) ou do cartão (crédito), usado
 *  pra derivar a cor de identidade e o label mostrado ("Banco Débito"/"Banco Cartão")
 */
data class CardMenuRow(val id: String, val label: String)

class CardsMenuAdapter : RecyclerView.Adapter<CardsMenuAdapter.ViewHolder>() {

    private var items: List<CardMenuRow> = emptyList()
    private var indicesCor: Map<String, Int> = emptyMap()

    fun atualizarLista(novosItens: List<CardMenuRow>) {
        items = novosItens
        indicesCor = EntityColorPalette.resolveIndices(novosItens.map { it.id })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], indicesCor)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val colorDot: View = view.findViewById(R.id.view_card_color_dot)
        private val textLabel: TextView = view.findViewById(R.id.text_card_menu_label)

        fun bind(row: CardMenuRow, indicesCor: Map<String, Int>) {
            textLabel.text = row.label
            val indice = indicesCor[row.id]
            val color = if (indice != null) {
                EntityColorPalette.colorAt(itemView.context, indice)
            } else {
                EntityColorPalette.colorFor(itemView.context, row.id)
            }
            colorDot.backgroundTintList = ColorStateList.valueOf(color)
        }
    }
}
