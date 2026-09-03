package com.luamuniz.dinlux.list

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Itens dentro de uma lista (ListDetailActivity), checklist com nome, quantidade e
 * preço opcionais. Marcado como feito: texto riscado no lugar de sempre (não move, não some)
 */
class ListItemAdapter(
    private val onToggleDone: (ShoppingListItem, Boolean) -> Unit,
    private val onClick: (ShoppingListItem) -> Unit
) : RecyclerView.Adapter<ListItemAdapter.ItemViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private var itens: List<ShoppingListItem> = emptyList()

    fun atualizarLista(novosItens: List<ShoppingListItem>) {
        itens = novosItens
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_entry, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itens[position])
    }

    override fun getItemCount(): Int = itens.size

    private fun formatarQuantidade(quantidade: Double): String {
        return if (quantidade == quantidade.toLong().toDouble()) {
            quantidade.toLong().toString()
        } else {
            quantidade.toString().replace(".", ",")
        }
    }

    private fun montarSubtitulo(item: ShoppingListItem): String? {
        val quantidade = item.quantity
        val preco = item.price
        return when {
            quantidade != null && preco != null -> {
                val subtotal = quantidade * preco
                "Qtd: ${formatarQuantidade(quantidade)} × ${currencyFormat.format(preco)} = ${currencyFormat.format(subtotal)}"
            }
            quantidade != null -> "Qtd: ${formatarQuantidade(quantidade)}"
            preco != null -> currencyFormat.format(preco)
            else -> null
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textName = view.findViewById<TextView>(R.id.text_list_item_name)
        private val textSubtitle = view.findViewById<TextView>(R.id.text_list_item_subtitle)
        private val checkbox = view.findViewById<CheckBox>(R.id.checkbox_list_item_done)

        fun bind(item: ShoppingListItem) {
            textName.text = item.name
            textName.paintFlags = if (item.done) {
                textName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                textName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            textName.alpha = if (item.done) 0.6f else 1f

            val subtitulo = montarSubtitulo(item)
            if (subtitulo != null) {
                textSubtitle.visibility = View.VISIBLE
                textSubtitle.text = subtitulo
                textSubtitle.paintFlags = if (item.done) {
                    textSubtitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    textSubtitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            } else {
                textSubtitle.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.done
            checkbox.setOnCheckedChangeListener { _, isChecked -> onToggleDone(item, isChecked) }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
