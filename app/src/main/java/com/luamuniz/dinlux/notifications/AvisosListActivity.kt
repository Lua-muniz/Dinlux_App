package com.luamuniz.dinlux.notifications

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

/**
 * "Lista de contatos" do módulo de Avisos: um item por banco que tem pelo menos uma
 * sessão (compra/economia) ainda não arquivada. O chat é um histórico corrido
 * (mensagens pagas e não pagas), não só perguntas em aberto, então um banco aparece aqui
 * mesmo com tudo em dia; ele só some da lista quando todas as sessões dele já tiverem sido arquivadas
 */
class AvisosListActivity : AppCompatActivity() {

    private val repository = AvisosRepository()
    private lateinit var buttonToBack: ImageButton
    private lateinit var recyclerContacts: RecyclerView
    private lateinit var textEmptyState: TextView
    private lateinit var adapter: AvisosContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_avisos_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        recyclerContacts = findViewById(R.id.recycler_avisos_contacts)
        textEmptyState = findViewById(R.id.text_empty_state)

        adapter = AvisosContactAdapter { contato -> abrirChat(contato) }
        recyclerContacts.layoutManager = LinearLayoutManager(this)
        recyclerContacts.adapter = adapter

        buttonToBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        carregarSecoes()
    }

    private fun carregarSecoes() {
        repository.carregarSecoes(
            onSuccess = { secoes -> exibirContatos(secoes) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirContatos(secoes: List<AvisoSecao>) {
        val contatos = secoes
            .groupBy { it.bankId }
            .map { (bankId, itens) ->
                AvisoContato(
                    bankId = bankId,
                    bankName = itens.first().bankName,
                    naoVistasCount = itens.sumOf { it.naoVistasCount },
                    ordenacao = itens.minOf { it.ordenacao }
                )
            }
            .sortedBy { it.ordenacao }

        adapter.atualizarLista(contatos)
        val vazio = contatos.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerContacts.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun abrirChat(contato: AvisoContato) {
        val intent = Intent(this, AvisosChatActivity::class.java)
        intent.putExtra(AvisosChatActivity.EXTRA_BANK_ID, contato.bankId)
        intent.putExtra(AvisosChatActivity.EXTRA_BANK_NAME, contato.bankName)
        startActivity(intent)
    }
}
