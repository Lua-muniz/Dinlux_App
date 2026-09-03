package com.luamuniz.dinlux.list

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Itens de uma lista, checklist com nome, quantidade e preço opcionais, e o total somado (dos itens com preço) no topo, como prévia de
 * gasto antes de sair de casa e Marcar/desmarcar o checkbox é imediato
 */
class ListDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "extra_list_id"
        const val EXTRA_LIST_TITLE = "extra_list_title"
    }

    private val repository = ListRepository()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private lateinit var listId: String
    private lateinit var listTitle: String
    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonAddItem: ImageButton
    private lateinit var titleListDetail: TextView
    private lateinit var textListProgress: TextView
    private lateinit var textListTotal: TextView
    private lateinit var textEmptyState: TextView
    private lateinit var recyclerItems: RecyclerView
    private lateinit var adapter: ListItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listId = intent.getStringExtra(EXTRA_LIST_ID) ?: ""
        listTitle = intent.getStringExtra(EXTRA_LIST_TITLE) ?: ""

        buttonToBack = findViewById(R.id.button_toBack)
        buttonAddItem = findViewById(R.id.button_add_item)
        titleListDetail = findViewById(R.id.title_list_detail)
        textListProgress = findViewById(R.id.text_list_progress)
        textListTotal = findViewById(R.id.text_list_total)
        textEmptyState = findViewById(R.id.text_empty_state)
        recyclerItems = findViewById(R.id.recycler_list_items)

        titleListDetail.text = listTitle

        adapter = ListItemAdapter(
            onToggleDone = { item, marcado -> alternarFeito(item, marcado) },
            onClick = { item -> mostrarOpcoesItem(item) }
        )
        recyclerItems.layoutManager = LinearLayoutManager(this)
        recyclerItems.adapter = adapter

        buttonToBack.setOnClickListener { finish() }
        buttonAddItem.setOnClickListener { mostrarDialogItem(existente = null) }
    }

    override fun onResume() {
        super.onResume()
        carregarItens()
    }

    private fun carregarItens() {
        repository.loadItems(
            listId = listId,
            onSuccess = { itens -> exibirItens(itens) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirItens(itens: List<ShoppingListItem>) {
        adapter.atualizarLista(itens)

        val vazio = itens.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerItems.visibility = if (vazio) View.GONE else View.VISIBLE

        if (vazio) {
            textListProgress.visibility = View.GONE
        } else {
            val feitos = itens.count { it.done }
            textListProgress.text = "Itens: $feitos / ${itens.size}"
            textListProgress.visibility = View.VISIBLE
        }

        val itensComPreco = itens.filter { it.price != null }
        if (itensComPreco.isEmpty()) {
            textListTotal.visibility = View.GONE
        } else {
            val subtotal = { item: ShoppingListItem -> (item.price ?: 0.0) * (item.quantity ?: 1.0) }
            val total = itensComPreco.sumOf(subtotal)
            val feito = itensComPreco.filter { it.done }.sumOf(subtotal)
            val falta = total - feito
            textListTotal.text = "Total: ${currencyFormat.format(total)}   " +
                "Feito: ${currencyFormat.format(feito)}   " +
                "Falta: ${currencyFormat.format(falta)}"
            textListTotal.visibility = View.VISIBLE
        }
    }

    private fun alternarFeito(item: ShoppingListItem, marcado: Boolean) {
        repository.setItemDone(
            itemId = item.id,
            done = marcado,
            onSuccess = { carregarItens() },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                carregarItens() // desfaz o toggle visual otimista do CheckBox em caso de erro
            }
        )
    }

    private fun mostrarOpcoesItem(item: ShoppingListItem) {
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(arrayOf("Editar", "Excluir")) { _, which ->
                when (which) {
                    0 -> mostrarDialogItem(existente = item)
                    1 -> confirmarExclusaoItem(item)
                }
            }
            .show()
    }

    private fun mostrarDialogItem(existente: ShoppingListItem?) {
        val paddingPx = (16 * resources.displayMetrics.density).toInt()

        val inputNome = EditText(this).apply {
            hint = "Nome do item (ex: Arroz, ou uma tarefa)"
            existente?.let { setText(it.name) }
        }
        val inputQuantidade = EditText(this).apply {
            hint = "Quantidade (opcional)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            existente?.quantity?.let { setText(it.toString().removeSuffix(".0")) }
        }
        val inputPreco = EditText(this).apply {
            hint = "Preço (opcional)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            existente?.price?.let { setText(String.format(Locale("pt", "BR"), "%.2f", it)) }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, 0, paddingPx, 0)
            addView(inputNome)
            addView(inputQuantidade)
            addView(inputPreco)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existente == null) "Novo item" else "Editar item")
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = inputNome.text.toString().trim()
                if (nome.isEmpty()) {
                    Toast.makeText(this, "Digite um nome pro item", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val quantidade = inputQuantidade.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val preco = inputPreco.text.toString().trim().replace(",", ".").toDoubleOrNull()

                if (existente == null) {
                    repository.addItem(
                        listId = listId,
                        name = nome,
                        quantity = quantidade,
                        price = preco,
                        onSuccess = { carregarItens() },
                        onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    repository.updateItem(
                        itemId = existente.id,
                        name = nome,
                        quantity = quantidade,
                        price = preco,
                        onSuccess = { carregarItens() },
                        onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusaoItem(item: ShoppingListItem) {
        AlertDialog.Builder(this)
            .setTitle("Excluir item")
            .setMessage("Tem certeza que deseja excluir \"${item.name}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                repository.deleteItem(
                    itemId = item.id,
                    onSuccess = { carregarItens() },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
