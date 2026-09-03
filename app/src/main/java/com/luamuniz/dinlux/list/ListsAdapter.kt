package com.luamuniz.dinlux.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

class ListsAdapter(
    private val onClick: (ShoppingList) -> Unit,
    private val onOptionsClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ListsAdapter.ListViewHolder>() {

    private var listas: List<ShoppingList> = emptyList()

    fun atualizarLista(novasListas: List<ShoppingList>) {
        listas = novasListas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping_list, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(listas[position])
    }

    override fun getItemCount(): Int = listas.size

    inner class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textTitle = view.findViewById<TextView>(R.id.text_shopping_list_title)
        private val buttonOptions = view.findViewById<ImageButton>(R.id.button_shopping_list_options)

        fun bind(lista: ShoppingList) {
            textTitle.text = lista.title
            itemView.setOnClickListener { onClick(lista) }
            buttonOptions.setOnClickListener { onOptionsClick(lista) }
        }
    }
}
