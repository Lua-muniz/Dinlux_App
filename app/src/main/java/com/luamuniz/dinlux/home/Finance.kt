package com.luamuniz.dinlux.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.excerpt.ExtratoAdapter
import com.luamuniz.dinlux.excerpt.ExtratoLinha
import com.luamuniz.dinlux.excerpt.StatementTransactionRecord
import com.luamuniz.dinlux.excerpt.StatementTransactionRepository
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.Card
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.finance.InsertBank
import com.luamuniz.dinlux.finance.InsertCard
import com.luamuniz.dinlux.simulation.SimulationCalculator
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationEntryRepository
import java.text.NumberFormat
import java.util.Locale

class Finance : AppCompatActivity() {

    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonConfig: ImageButton

    private lateinit var buttonNextCard: ImageButton
    private lateinit var buttonPreviousCard: ImageButton
    private lateinit var buttonNextBank: ImageButton
    private lateinit var buttonPreviousBank: ImageButton
    private lateinit var titleSaldo: View
    private lateinit var valueSaldo: TextView
    private lateinit var banckName: TextView
    private lateinit var ivIconeBanco: View
    private lateinit var cvCartao: View

    private lateinit var tvInfoCartao: TextView
    private lateinit var tvCardLabel: TextView
    private lateinit var tvCardLimit: TextView
    private lateinit var tvCardClosing: TextView
    private lateinit var tvCardDue: TextView
    private lateinit var tvCardCounter: TextView

    private lateinit var titleExtrato: View
    private lateinit var textExtratoEstado: TextView
    private lateinit var containerExtrato: View
    private lateinit var recyclerExtrato: RecyclerView

    private lateinit var overlayLoadingFinance: View
    private lateinit var progressFinance: ProgressBar

    private val extratoAdapter = ExtratoAdapter()

    private var banks: MutableList<Bank> = mutableListOf()
    private var currentBankIndex = 0
    private var currentCardIndex = 0
    private val financeHandler = Handler(Looper.getMainLooper())
    private var financeRequestId = 0
    private var comprasCreditoAtivas: List<SimulationEntry> = emptyList()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val db = FirebaseFirestore.getInstance()
    private val entryRepository = SimulationEntryRepository()
    private val statementTransactionRepository = StatementTransactionRepository()
    private val bankRepository = BankRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_finance)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarViews()

        buttonToBack.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        // editar saldo
        valueSaldo.setOnClickListener { mostrarDialogEditarSaldo() }

        // editar / adicionar / excluir banco
        banckName.setOnClickListener { mostrarOpcoesBanco() }

        // editar / adicionar / excluir cartão
        buttonConfig.setOnClickListener { mostrarOpcoesCartao() }

        buttonNextCard.setOnClickListener { mudarCartao(1) }
        buttonPreviousCard.setOnClickListener { mudarCartao(-1) }
        buttonNextBank.setOnClickListener { mudarBanco(1) }
        buttonPreviousBank.setOnClickListener { mudarBanco(-1) }

        carregarBancos()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega ao voltar de telas de inserção (novo banco / novo cartão / editar
        // cartão) e também ao voltar de uma confirmação no módulo de Avisos
        if (::banckName.isInitialized) carregarBancos()
    }

    private fun inicializarViews() {
        buttonToBack = findViewById(R.id.button_toBack)
        buttonConfig = findViewById(R.id.button_config)

        titleSaldo = findViewById(R.id.title_saldo)
        valueSaldo = findViewById(R.id.value_saldo)
        banckName = findViewById(R.id.banck_name)
        ivIconeBanco = findViewById(R.id.ivIconeBanco)
        buttonPreviousCard = findViewById(R.id.button_previous_card)
        buttonNextCard = findViewById(R.id.button_next_card)
        buttonPreviousBank = findViewById(R.id.button_previous_bank)
        buttonNextBank = findViewById(R.id.button_next_bank)
        cvCartao = findViewById(R.id.cvCartao)

        tvInfoCartao = findViewById(R.id.tvInfoCartao)
        tvCardLabel = findViewById(R.id.tvCardLabel)
        tvCardLimit = findViewById(R.id.tvCardLimit)
        tvCardClosing = findViewById(R.id.tvCardClosing)
        tvCardDue = findViewById(R.id.tvCardDue)
        tvCardCounter = findViewById(R.id.tvCardCounter)

        titleExtrato = findViewById(R.id.title_extrato)
        textExtratoEstado = findViewById(R.id.text_extrato_estado)
        containerExtrato = findViewById(R.id.container_extrato)
        recyclerExtrato = findViewById(R.id.recycler_extrato)

        overlayLoadingFinance = findViewById(R.id.overlay_loading_finance)
        progressFinance = findViewById(R.id.progress_finance)

        recyclerExtrato.layoutManager = LinearLayoutManager(this)
        recyclerExtrato.adapter = extratoAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        // Evita que um postDelayed do spinner de carregamento dispare depois da Activity já ter sido destruída
        financeHandler.removeCallbacksAndMessages(null)
    }

    private fun carregarBancos() {
        val requestId = ++financeRequestId
        overlayLoadingFinance.visibility = View.VISIBLE
        progressFinance.visibility = View.VISIBLE

        var tempoMinimoAtingido = false
        var aoFicarPronto: (() -> Unit)? = null
        financeHandler.postDelayed({
            if (requestId != financeRequestId) return@postDelayed
            tempoMinimoAtingido = true
            aoFicarPronto?.invoke()
        }, FINANCE_LOADING_MIN_MS)

        fun exibirQuandoPronto(aplicar: () -> Unit) {
            if (requestId != financeRequestId) return // tela já foi recarregada/destruída de novo
            val revelar = {
                aplicar()
                overlayLoadingFinance.visibility = View.GONE
                progressFinance.visibility = View.GONE
            }
            if (tempoMinimoAtingido) revelar() else aoFicarPronto = revelar
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            exibirQuandoPronto { mostrarEstadoSemBanco() }
            return
        }

        db.collection("users").document(uid).collection("banks")
            .get()
            .addOnSuccessListener { snapshot ->
                banks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Bank::class.java)?.apply { id = doc.id }
                }.toMutableList()

                if (banks.isEmpty()) {
                    exibirQuandoPronto { mostrarEstadoSemBanco() }
                } else {
                    if (currentBankIndex >= banks.size) currentBankIndex = 0
                    carregarComprasCreditoAtivas {
                        exibirQuandoPronto { mostrarBanco(currentBankIndex) }
                    }
                }
            }
            .addOnFailureListener {
                exibirQuandoPronto { mostrarEstadoSemBanco() }
            }
    }

    // Carrega as compras em crédito de todas as simulações ativas (qualquer banco/cartão)
    // usadas em textoLimite() para calcular o limite disponível de cada cartão
    // (SimulationCalculator.calcularLimiteDisponivel: limite cadastrado menos o que ainda
    // está em aberto nas parcelas não confirmadas, menos também Card.usedAmount). Falha
    // silenciosa: se não conseguir carregar, a tela cai de volta pro limite total
    // cadastrado em vez de travar. Lógica em si mora em
    // SimulationEntryRepository.loadActiveCreditPurchases
    private fun carregarComprasCreditoAtivas(aoTerminar: () -> Unit) {
        entryRepository.loadActiveCreditPurchases { compras ->
            comprasCreditoAtivas = compras
            aoTerminar()
        }
    }

    private fun mostrarEstadoSemBanco() {
        banckName.text = "Criar Banco"
        valueSaldo.text = currencyFormat.format(0.0)
        tvInfoCartao.visibility = View.VISIBLE
        tvCardLabel.visibility = View.GONE
        tvCardLimit.visibility = View.GONE
        tvCardClosing.visibility = View.GONE
        tvCardDue.visibility = View.GONE
        tvCardCounter.visibility = View.GONE
        buttonNextCard.isEnabled = false
        buttonPreviousCard.isEnabled = false
        buttonNextBank.isEnabled = false
        buttonPreviousBank.isEnabled = false

        titleExtrato.visibility = View.GONE
        textExtratoEstado.visibility = View.GONE
        containerExtrato.visibility = View.GONE
    }

    private fun mostrarBanco(index: Int) {
        if (banks.isEmpty()) return
        currentBankIndex = index
        val bank = banks[currentBankIndex]

        banckName.text = bank.name
        valueSaldo.text = currencyFormat.format(bank.debit)

        buttonNextBank.isEnabled = banks.size > 1
        buttonPreviousBank.isEnabled = banks.size > 1

        currentCardIndex = 0
        mostrarCartao()

        titleExtrato.visibility = View.VISIBLE
        carregarExtrato(bank.id)
    }

    // Extrato importado, mostra todos os lançamentos já salvos pra
    // esse banco (não só uma prévia), mais recente primeiro. Recarrega toda vez que o
    // banco exibido muda (troca de banco no carrossel, ou onResume() ao voltar de uma
    // importação nova em ImportExtratoActivity)
    private fun carregarExtrato(bankId: String) {
        statementTransactionRepository.loadTransactions(
            bankId = bankId,
            onSuccess = { transacoes -> exibirExtrato(transacoes) },
            onError = { mostrarEstadoExtrato("Não foi possível carregar o extrato.") }
        )
    }

    // Tabela de verdade (Data | Valor | Descrição, ExtratoAdapter)
    private fun exibirExtrato(transacoesBrutas: List<StatementTransactionRecord>) {
        if (transacoesBrutas.isEmpty()) {
            mostrarEstadoExtrato("Nenhum lançamento importado ainda.")
            return
        }

        val transacoes = transacoesBrutas.sortedByDescending { it.date }
        extratoAdapter.atualizar(
            transacoes.map { ExtratoLinha(it.date, it.description, it.amount, it.credit) }
        )
        textExtratoEstado.visibility = View.GONE
        containerExtrato.visibility = View.VISIBLE
    }

    private fun mostrarEstadoExtrato(mensagem: String) {
        textExtratoEstado.text = mensagem
        textExtratoEstado.visibility = View.VISIBLE
        containerExtrato.visibility = View.GONE
    }

    private fun mostrarCartao() {
        if (banks.isEmpty()) return
        val cards = banks[currentBankIndex].cards

        if (cards.isEmpty()) {
            tvInfoCartao.visibility = View.VISIBLE
            tvCardLabel.visibility = View.GONE
            tvCardLimit.visibility = View.GONE
            tvCardClosing.visibility = View.GONE
            tvCardDue.visibility = View.GONE
            tvCardCounter.visibility = View.GONE
            buttonNextCard.isEnabled = false
            buttonPreviousCard.isEnabled = false
            return
        }

        buttonNextCard.isEnabled = cards.size > 1
        buttonPreviousCard.isEnabled = cards.size > 1

        val card = cards[currentCardIndex]

        tvInfoCartao.visibility = View.GONE
        tvCardLabel.visibility = View.VISIBLE
        tvCardLimit.visibility = View.VISIBLE
        tvCardClosing.visibility = View.VISIBLE
        tvCardDue.visibility = View.VISIBLE

        tvCardLabel.text = card.label
        tvCardLimit.text = textoLimite(card)
        tvCardClosing.text = "Fechamento: dia ${card.closingDay}"
        tvCardDue.text = "Vencimento: dia ${card.dueDay}"

        if (cards.size > 1) {
            tvCardCounter.visibility = View.VISIBLE
            tvCardCounter.text = "${currentCardIndex + 1}/${cards.size}"
        } else {
            tvCardCounter.visibility = View.GONE
        }
    }

    private fun textoLimite(card: Card): String {
        val bank = banks[currentBankIndex]
        val comprasDoCartao = comprasCreditoAtivas.filter { it.bankId == bank.id && it.cardId == card.id }
        val linhaTotal = "Limite: ${currencyFormat.format(card.limit)}"
        if (comprasDoCartao.isEmpty() && card.usedAmount <= 0.0) {
            return linhaTotal
        }
        val disponivel = SimulationCalculator.calcularLimiteDisponivel(card.limit, comprasDoCartao, card.usedAmount)
        return "$linhaTotal\nLimite disponível: ${currencyFormat.format(disponivel.coerceAtLeast(0.0))}"
    }

    private fun mudarCartao(direction: Int) {
        if (banks.isEmpty()) return
        val cards = banks[currentBankIndex].cards
        if (cards.isEmpty()) return
        currentCardIndex = (currentCardIndex + direction + cards.size) % cards.size
        mostrarCartao()
    }

    private fun mudarBanco(direction: Int) {
        if (banks.isEmpty()) return
        val newIndex = (currentBankIndex + direction + banks.size) % banks.size
        mostrarBanco(newIndex)
    }

    private fun mostrarDialogEditarSaldo() {
        if (banks.isEmpty()) {
            adicionarBanco()
            return
        }
        val bank = banks[currentBankIndex]

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale("pt", "BR"), "%.2f", bank.debit))
        }

        AlertDialog.Builder(this)
            .setTitle("Editar saldo")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoValor = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoValor != null) {
                    bank.debit = novoValor
                    db.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                        .collection("banks").document(bank.id)
                        .update("debit", novoValor)
                    valueSaldo.text = currencyFormat.format(novoValor)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpcoesBanco() {
        if (banks.isEmpty()) {
            adicionarBanco()
            return
        }
        val opcoes = arrayOf("Renomear", "Criar", "Excluir Extrato", "Excluir")
        AlertDialog.Builder(this)
            .setTitle("Banco")
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> mostrarDialogEditarNomeBanco()
                    1 -> adicionarBanco()
                    2 -> confirmarExcluirExtrato()
                    3 -> confirmarExcluirBanco()
                }
            }
            .show()
    }

    private fun mostrarDialogEditarNomeBanco() {
        val bank = banks[currentBankIndex]
        val input = EditText(this).apply { setText(bank.name) }

        AlertDialog.Builder(this)
            .setTitle("Editar nome do banco")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = input.text.toString().trim()
                if (novoNome.isNotEmpty()) {
                    bank.name = novoNome
                    db.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                        .collection("banks").document(bank.id)
                        .update("name", novoNome)
                    banckName.text = novoNome
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun adicionarBanco() {
        startActivity(Intent(this, InsertBank::class.java))
    }

    private fun confirmarExcluirExtrato() {
        val bank = banks[currentBankIndex]
        AlertDialog.Builder(this)
            .setTitle("Excluir extrato")
            .setMessage(
                "Tem certeza que deseja excluir todo o extrato importado de \"${bank.name}\"? " +
                    "Essa ação não pode ser desfeita. Os bancos, cartões e simulações de " +
                    "\"${bank.name}\" continuam intactos, só o extrato importado é apagado."
            )
            .setPositiveButton("Excluir") { _, _ -> excluirExtratoAtual() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirExtratoAtual() {
        val bank = banks[currentBankIndex]
        statementTransactionRepository.apagarExtratoDoBanco(
            bankId = bank.id,
            onSuccess = { mostrarEstadoExtrato("Nenhum lançamento importado ainda.") },
            onError = { message ->
                Toast.makeText(this, "Não foi possível excluir o extrato: $message", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun confirmarExcluirBanco() {
        AlertDialog.Builder(this)
            .setTitle("Excluir banco")
            .setMessage(
                "Tem certeza que deseja excluir \"${banks[currentBankIndex].name}\"? Essa " +
                    "ação não pode ser desfeita e apaga junto tudo relacionado a esse " +
                    "banco: lançamentos e Grupos em Simulações, e o extrato importado dele."
            )
            .setPositiveButton("Excluir") { _, _ -> excluirBancoAtual() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun excluirBancoAtual() {
        val bank = banks[currentBankIndex]
        entryRepository.deleteEntriesAndGroupsForBank(
            bankId = bank.id,
            onSuccess = {
                statementTransactionRepository.apagarExtratoDoBanco(
                    bankId = bank.id,
                    onSuccess = { apagarDocumentoDoBanco(bank) },
                    onError = { message ->
                        Toast.makeText(this, "Não foi possível excluir o banco: $message", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { message ->
                Toast.makeText(this, "Não foi possível excluir o banco: $message", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun apagarDocumentoDoBanco(bank: Bank) {
        db.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
            .collection("banks").document(bank.id)
            .delete()
        // Atualiza a tela na hora: a exclusão já foi gravada no cache local (offline ou
        // não) e sincroniza sozinha quando conectar, sem precisar esperar o servidor
        banks.removeAt(currentBankIndex)
        if (banks.isEmpty()) {
            mostrarEstadoSemBanco()
        } else {
            currentBankIndex = 0
            mostrarBanco(currentBankIndex)
        }
    }

    private fun mostrarOpcoesCartao() {
        if (banks.isEmpty()) {
            adicionarBanco()
            return
        }
        val temCartao = banks[currentBankIndex].cards.isNotEmpty()
        val opcoes = if (temCartao) {
            arrayOf("Editar", "Criar", "Limite Disponível", "Excluir")
        } else {
            arrayOf("Criar")
        }

        AlertDialog.Builder(this)
            .setTitle("Cartão de crédito")
            .setItems(opcoes) { _, which ->
                when (opcoes[which]) {
                    "Editar" -> editarCartao()
                    "Criar" -> adicionarCartao()
                    "Limite Disponível" -> editarLimiteDisponivel()
                    "Excluir" -> confirmarExcluirCartao()
                }
            }
            .show()
    }

    private fun editarLimiteDisponivel() {
        val bank = banks[currentBankIndex]
        val card = bank.cards.getOrNull(currentCardIndex) ?: return
        val comprasDoCartao = comprasCreditoAtivas.filter { it.bankId == bank.id && it.cardId == card.id }
        val disponivelAtual = SimulationCalculator.calcularLimiteDisponivel(card.limit, comprasDoCartao, card.usedAmount)

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale("pt", "BR"), "%.2f", disponivelAtual))
        }
        AlertDialog.Builder(this)
            .setTitle("Alterar Limite Disponível")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoDisponivel = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoDisponivel == null) {
                    Toast.makeText(this, "Informe um valor válido.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (novoDisponivel > card.limit) {
                    Toast.makeText(this, "O limite disponível não pode ser maior que o limite total do cartão.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val diferenca = disponivelAtual - novoDisponivel
                bankRepository.atualizarLimiteUsadoCartao(
                    bankId = bank.id,
                    cardId = card.id,
                    novoUsedAmount = card.usedAmount + diferenca,
                    onSuccess = { carregarBancos() },
                    onError = { erro ->
                        Toast.makeText(this, "Não foi possível atualizar o limite: $erro", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun adicionarCartao() {
        val bank = banks[currentBankIndex]
        val intent = Intent(this, InsertCard::class.java)
        intent.putExtra(InsertCard.EXTRA_BANK_ID, bank.id)
        startActivity(intent)
    }

    private fun editarCartao() {
        val bank = banks[currentBankIndex]
        val card = bank.cards.getOrNull(currentCardIndex) ?: return

        val intent = Intent(this, InsertCard::class.java)
        intent.putExtra(InsertCard.EXTRA_BANK_ID, bank.id)
        intent.putExtra(InsertCard.EXTRA_CARD_ID, card.id)
        intent.putExtra(InsertCard.EXTRA_CARD_LABEL, card.label)
        intent.putExtra(InsertCard.EXTRA_CARD_LIMIT, card.limit)
        intent.putExtra(InsertCard.EXTRA_CARD_CLOSING_DAY, card.closingDay)
        intent.putExtra(InsertCard.EXTRA_CARD_DUE_DAY, card.dueDay)
        intent.putExtra(InsertCard.EXTRA_CARD_HAS_INTEREST, card.hasInterest)
        intent.putExtra(InsertCard.EXTRA_CARD_INTEREST_RATE, card.interestRate)
        startActivity(intent)
    }

    private fun confirmarExcluirCartao() {
        AlertDialog.Builder(this)
            .setTitle("Excluir cartão")
            .setMessage(
                "Tem certeza que deseja excluir este cartão? Essa ação não pode ser " +
                    "desfeita e apaga junto tudo relacionado a ele: os lançamentos e " +
                    "Grupos em Simulações que usam esse cartão."
            )
            .setPositiveButton("Excluir") { _, _ -> excluirCartaoAtual() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Mesma cascata de excluirBancoAtual() (ver KDoc lá), só que restrita ao cardId desse
    // cartão específico, lançamentos de débito/economia do BANCO continuam intactos, só
    // o que usava esse cartão de crédito é apagado junto com ele
    private fun excluirCartaoAtual() {
        val bank = banks[currentBankIndex]
        val cards = bank.cards.toMutableList()
        if (currentCardIndex >= cards.size) return
        val cartaoExcluido = cards[currentCardIndex]

        entryRepository.deleteEntriesAndGroupsForBank(
            bankId = bank.id,
            cardId = cartaoExcluido.id,
            onSuccess = {
                cards.removeAt(currentCardIndex)
                bank.cards = cards

                db.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .collection("banks").document(bank.id)
                    .update("cards", cards)
                currentCardIndex = 0
                mostrarCartao()
            },
            onError = { message ->
                Toast.makeText(this, "Não foi possível excluir o cartão: $message", Toast.LENGTH_LONG).show()
            }
        )
    }

    private companion object {
        const val FINANCE_LOADING_MIN_MS = 1000L
    }
}
