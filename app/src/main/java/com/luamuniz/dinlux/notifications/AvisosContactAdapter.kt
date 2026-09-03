package com.luamuniz.dinlux.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R

/** Adapter da lista de contatos (bancos) do módulo de Avisos. */
class AvisosContactAdapter(
    private val onClick: (AvisoContato) -> Unit
) : RecyclerView.Adapter<AvisosContactAdapter.ContatoViewHolder>() {

    private var contatos: List<AvisoContato> = emptyList()

    fun atualizarLista(novosContatos: List<AvisoContato>) {
        contatos = novosContatos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContatoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_aviso_contact, parent, false)
        return ContatoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContatoViewHolder, position: Int) {
        holder.bind(contatos[position])
    }

    override fun getItemCount(): Int = contatos.size

    inner class ContatoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textName = view.findViewById<TextView>(R.id.text_aviso_contact_name)
        private val textBadge = view.findViewById<TextView>(R.id.text_aviso_contact_badge)

        fun bind(contato: AvisoContato) {
            textName.text = contato.bankName

            if (contato.naoVistasCount > 0) {
                textBadge.text = contato.naoVistasCount.toString()
                textBadge.visibility = View.VISIBLE
            } else {
                textBadge.visibility = View.GONE
            }
            itemView.setOnClickListener { onClick(contato) }
        }
    }
}
