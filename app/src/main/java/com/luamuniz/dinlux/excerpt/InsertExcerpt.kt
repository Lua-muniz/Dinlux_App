package com.luamuniz.dinlux.excerpt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository

/**
 * Ponto de entrada da leitura de extrato bancário. Botão "Importar Extrato" da Home
 *
 * Mostra a lista de bancos cadastrados pelo usuário, escolher o banco aqui é o que evita
 * o usuário jogar o extrato de um banco em cima de outro por engano. Tocar num banco abre
 * ImportExtratoActivity, que já existia
 */
class InsertExcerpt : AppCompatActivity() {

    private val bankRepository = BankRepository()
    private lateinit var buttonToBack: ImageButton
    private lateinit var recyclerBanks: RecyclerView
    private lateinit var textEmptyState: TextView
    private lateinit var adapter: InsertExcerptBankAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insert_excerpt)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        recyclerBanks = findViewById(R.id.recycler_excerpt_banks)
        textEmptyState = findViewById(R.id.text_empty_state)

        adapter = InsertExcerptBankAdapter { bank -> abrirImportarExtrato(bank) }
        recyclerBanks.layoutManager = LinearLayoutManager(this)
        recyclerBanks.adapter = adapter

        buttonToBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        carregarBancos()
    }

    private fun carregarBancos() {
        bankRepository.loadBanks(
            onSuccess = { banks -> exibirBancos(banks) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirBancos(banks: List<Bank>) {
        adapter.atualizarLista(banks)
        val vazio = banks.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerBanks.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun abrirImportarExtrato(bank: Bank) {
        val intent = Intent(this, ImportExtratoActivity::class.java)
        intent.putExtra(ImportExtratoActivity.EXTRA_BANK_ID, bank.id)
        intent.putExtra(ImportExtratoActivity.EXTRA_BANK_NAME, bank.name)
        intent.putExtra(ImportExtratoActivity.EXTRA_BANK_CODE, bank.bankCode)
        startActivity(intent)
    }
}
