package com.luamuniz.dinlux.list

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
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
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.simulation.SimulationCalculator
import com.luamuniz.dinlux.simulation.SimulationEntryRepository
import java.text.NumberFormat
import java.util.Locale

/**
 * Lista de listas do usuário
 */
class ListsActivity : AppCompatActivity() {

    private val repository = ListRepository()
    private val bankRepository = BankRepository()
    private val entryRepository = SimulationEntryRepository()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    // Uma linha do seletor de "Lançar": banco (débito) ou banco+cartão (crédito)
    // [disponivel] já é o valor livre de verdade (saldo do banco, ou limite disponível do
    // cartão descontando parcelas de Simulação + usedAmount); [usedAmountAtual] só é
    // relevante pra cartão (cardId != null)
    private data class LancamentoOpcao(
        val label: String,
        val nomeDestino: String,
        val disponivel: Double,
        val bankId: String,
        val cardId: String?,
        val usedAmountAtual: Double = 0.0
    )

    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonAddList: ImageButton
    private lateinit var recyclerLists: RecyclerView
    private lateinit var textEmptyState: TextView
    private lateinit var adapter: ListsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lists)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        buttonAddList = findViewById(R.id.button_add_list)
        recyclerLists = findViewById(R.id.recycler_lists)
        textEmptyState = findViewById(R.id.text_empty_state)

        adapter = ListsAdapter(
            onClick = { lista -> abrirDetalhe(lista) },
            onOptionsClick = { lista -> mostrarOpcoes(lista) }
        )
        recyclerLists.layoutManager = LinearLayoutManager(this)
        recyclerLists.adapter = adapter

        buttonToBack.setOnClickListener { finish() }
        buttonAddList.setOnClickListener { mostrarDialogCriarLista() }
    }

    override fun onResume() {
        super.onResume()
        carregarListas()
    }

    private fun carregarListas() {
        repository.loadLists(
            onSuccess = { listas -> exibirListas(listas) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirListas(listas: List<ShoppingList>) {
        adapter.atualizarLista(listas)
        val vazio = listas.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerLists.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun abrirDetalhe(lista: ShoppingList) {
        val intent = Intent(this, ListDetailActivity::class.java)
        intent.putExtra(ListDetailActivity.EXTRA_LIST_ID, lista.id)
        intent.putExtra(ListDetailActivity.EXTRA_LIST_TITLE, lista.title)
        startActivity(intent)
    }

    private fun mostrarOpcoes(lista: ShoppingList) {
        AlertDialog.Builder(this)
            .setTitle(lista.title)
            .setItems(arrayOf("Renomear", "Excluir", "Lançar")) { _, which ->
                when (which) {
                    0 -> mostrarDialogRenomear(lista)
                    1 -> confirmarExclusao(lista)
                    2 -> iniciarLancamento(lista)
                }
            }
            .show()
    }

    // "Lançar" desconta só o valor já feito da lista (itens com preço marcados como
    // concluídos), não o Total inteiro, senão descontaria coisa que ainda nem foi comprada
    private fun iniciarLancamento(lista: ShoppingList) {
        repository.loadItems(
            listId = lista.id,
            onSuccess = { itens ->
                val feito = itens.filter { it.price != null && it.done }
                    .sumOf { (it.price ?: 0.0) * (it.quantity ?: 1.0) }
                if (feito <= 0.0) {
                    Toast.makeText(
                        this,
                        "Essa lista não tem nenhum item concluído com preço pra lançar.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mostrarEscolhaDestino(lista, feito)
                }
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun mostrarEscolhaDestino(lista: ShoppingList, valor: Double) {
        bankRepository.loadBanks(
            onSuccess = { banks ->
                if (banks.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Nenhum banco cadastrado ainda. Cadastre um banco na tela de Finanças primeiro.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@loadBanks
                }
                entryRepository.loadActiveCreditPurchases { comprasCreditoAtivas ->
                    val opcoes = banks.flatMap { bank ->
                        val debito = LancamentoOpcao(
                            label = "${bank.name} — Débito (${currencyFormat.format(bank.debit)})",
                            nomeDestino = "${bank.name} — Débito",
                            disponivel = bank.debit,
                            bankId = bank.id,
                            cardId = null
                        )
                        val cartoes = bank.cards.map { card ->
                            val comprasDoCartao = comprasCreditoAtivas.filter {
                                it.bankId == bank.id && it.cardId == card.id
                            }
                            val disponivel = SimulationCalculator.calcularLimiteDisponivel(
                                card.limit, comprasDoCartao, card.usedAmount
                            )
                            LancamentoOpcao(
                                label = "${bank.name} — ${card.label} (${currencyFormat.format(disponivel)} disponível)",
                                nomeDestino = "${bank.name} — ${card.label}",
                                disponivel = disponivel,
                                bankId = bank.id,
                                cardId = card.id,
                                usedAmountAtual = card.usedAmount
                            )
                        }
                        listOf(debito) + cartoes
                    }
                    val rotulos = opcoes.map { it.label }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Lançar ${currencyFormat.format(valor)} (itens concluídos) em qual banco/cartão?")
                        .setItems(rotulos) { _, which -> avaliarDisponibilidade(lista, valor, opcoes[which]) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun avaliarDisponibilidade(lista: ShoppingList, valor: Double, opcao: LancamentoOpcao) {
        if (opcao.disponivel >= valor) {
            confirmarLancamento(lista, valor, opcao)
        } else {
            mostrarDisponibilidadeInsuficiente(valor, opcao)
        }
    }

    private fun confirmarLancamento(lista: ShoppingList, valor: Double, opcao: LancamentoOpcao) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar lançamento")
            .setMessage(
                "Descontar ${currencyFormat.format(valor)} (itens já concluídos) de \"${lista.title}\" " +
                    "de ${opcao.nomeDestino}? Essa ação não pode ser desfeita automaticamente."
            )
            .setPositiveButton("Lançar") { _, _ -> aplicarLancamento(opcao, valor) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // O disponível calculado (saldo do banco / limite do cartão) é menor que o valor a
    // lançar, em vez de travar, oferece corrigir o valor real (o registrado pode estar
    // desatualizado)
    private fun mostrarDisponibilidadeInsuficiente(valor: Double, opcao: LancamentoOpcao) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Ex: 180,00"
        }
        AlertDialog.Builder(this)
            .setTitle("Disponível insuficiente")
            .setMessage(
                "O disponível calculado de \"${opcao.nomeDestino}\" (${currencyFormat.format(opcao.disponivel)}) " +
                    "é menor que ${currencyFormat.format(valor)}. Qual é o valor disponível de verdade " +
                    "(saldo do banco ou limite disponível do cartão)?"
            )
            .setView(input)
            .setPositiveButton("Atualizar e lançar") { _, _ ->
                val novoDisponivel = input.text.toString().trim().replace(",", ".").toDoubleOrNull()
                if (novoDisponivel == null) {
                    Toast.makeText(this, "Informe um valor válido.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                aplicarCorrecaoEDescontar(opcao, valor, novoDisponivel)
            }
            .setNegativeButton("Fechar", null) // fechar = nada acontece, nenhum desconto em lugar nenhum
            .show()
    }

    // Caminho normal: disponível já é suficiente, só aplica o desconto
    private fun aplicarLancamento(opcao: LancamentoOpcao, valor: Double) {
        if (opcao.cardId == null) {
            bankRepository.descontarSaldo(
                bankId = opcao.bankId,
                valor = valor,
                onSuccess = { avisarLancamentoOk(opcao, valor) },
                onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
            )
        } else {
            bankRepository.atualizarLimiteUsadoCartao(
                bankId = opcao.bankId,
                cardId = opcao.cardId,
                novoUsedAmount = opcao.usedAmountAtual + valor,
                onSuccess = { avisarLancamentoOk(opcao, valor) },
                onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun aplicarCorrecaoEDescontar(opcao: LancamentoOpcao, valor: Double, novoDisponivel: Double) {
        if (opcao.cardId == null) {
            bankRepository.updateBankDebit(
                bankId = opcao.bankId,
                novoValor = novoDisponivel - valor,
                onSuccess = { avisarLancamentoOk(opcao, valor) },
                onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
            )
        } else {
            val delta = opcao.disponivel - novoDisponivel
            bankRepository.atualizarLimiteUsadoCartao(
                bankId = opcao.bankId,
                cardId = opcao.cardId,
                novoUsedAmount = opcao.usedAmountAtual + delta + valor,
                onSuccess = { avisarLancamentoOk(opcao, valor) },
                onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun avisarLancamentoOk(opcao: LancamentoOpcao, valor: Double) {
        Toast.makeText(
            this,
            "Lançado! ${currencyFormat.format(valor)} (itens concluídos) descontado de ${opcao.nomeDestino}.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun mostrarDialogCriarLista() {
        val input = EditText(this).apply { hint = "" }
        AlertDialog.Builder(this)
            .setTitle("Nova lista")
            .setView(input)
            .setPositiveButton("Criar") { _, _ ->
                val titulo = input.text.toString().trim()
                if (titulo.isNotEmpty()) {
                    repository.createList(
                        title = titulo,
                        onSuccess = { carregarListas() },
                        onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogRenomear(lista: ShoppingList) {
        val input = EditText(this).apply { setText(lista.title) }
        AlertDialog.Builder(this)
            .setTitle("Renomear lista")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoTitulo = input.text.toString().trim()
                if (novoTitulo.isNotEmpty()) {
                    repository.renameList(
                        id = lista.id,
                        newTitle = novoTitulo,
                        onSuccess = { carregarListas() },
                        onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusao(lista: ShoppingList) {
        AlertDialog.Builder(this)
            .setTitle("Excluir lista")
            .setMessage("Tem certeza que deseja excluir \"${lista.title}\" e todos os seus itens? Essa ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                repository.deleteList(
                    id = lista.id,
                    onSuccess = { carregarListas() },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
