package com.luamuniz.dinlux.excerpt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.Bank

/** Lista de bancos cadastrados na tela de "Importar Extrato"
 **/
class InsertExcerptBankAdapter(
    private val onClick: (Bank) -> Unit
) : RecyclerView.Adapter<InsertExcerptBankAdapter.BankViewHolder>() {

    private var banks: List<Bank> = emptyList()

    fun atualizarLista(novosBancos: List<Bank>) {
        banks = novosBancos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_excerpt_bank, parent, false)
        return BankViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankViewHolder, position: Int) {
        holder.bind(banks[position])
    }

    override fun getItemCount(): Int = banks.size

    inner class BankViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textName = view.findViewById<TextView>(R.id.text_excerpt_bank_name)

        fun bind(bank: Bank) {
            textName.text = bank.name
            itemView.setOnClickListener { onClick(bank) }
        }
    }
}
