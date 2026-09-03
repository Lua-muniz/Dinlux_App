package com.luamuniz.dinlux.notifications

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.simulation.PaymentMethod
import com.luamuniz.dinlux.simulation.SimulationCalculator
import com.luamuniz.dinlux.simulation.SimulationEntryRepository
import com.luamuniz.dinlux.simulation.SimulationEntryType
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class AvisosChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BANK_ID = "bankId"
        const val EXTRA_BANK_NAME = "bankName"
    }

    private val repository = AvisosRepository()
    private val bankRepository = BankRepository()
    private val entryRepository = SimulationEntryRepository()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM/yyyy", Locale("pt", "BR"))

    private lateinit var buttonToBack: ImageButton
    private lateinit var textTitle: TextView
    private lateinit var textSaldoProjetado: TextView
    private lateinit var headerLimitesCartoes: LinearLayout
    private lateinit var buttonToggleLimitesCartoes: ImageButton
    private lateinit var containerLimitesCartoes: LinearLayout
    private lateinit var recyclerChat: RecyclerView
    private lateinit var textEmptyState: TextView

    private lateinit var adapter: AvisosChatAdapter

    private var bankId: String = ""
    private var bankName: String = ""

    private var saldoProjetado: Double = 0.0
    private var saldoCarregado = false
    private var limitesExpandido = true

    private data class LimiteCartaoProjetado(val cardId: String, val cardLabel: String, var limiteProjetado: Double)
    private val limitesPorCartao = mutableMapOf<String, LimiteCartaoProjetado>()
    private val linhasLimiteCartao = mutableMapOf<String, TextView>()

    private data class AcaoRealizada(val secao: AvisoSecao, val mensagem: AvisoMensagem, val confirmarAplicado: Boolean)
    private val historicoDeAcoes = mutableListOf<AcaoRealizada>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_avisos_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bankId = intent.getStringExtra(EXTRA_BANK_ID) ?: ""
        bankName = intent.getStringExtra(EXTRA_BANK_NAME) ?: ""

        buttonToBack = findViewById(R.id.button_toBack)
        textTitle = findViewById(R.id.title_avisos_chat)
        textSaldoProjetado = findViewById(R.id.text_saldo_projetado)
        headerLimitesCartoes = findViewById(R.id.header_limites_cartoes)
        buttonToggleLimitesCartoes = findViewById(R.id.button_toggle_limites_cartoes)
        containerLimitesCartoes = findViewById(R.id.container_limites_cartoes)
        recyclerChat = findViewById(R.id.recycler_avisos_chat)
        textEmptyState = findViewById(R.id.text_empty_state)

        textTitle.text = bankName
        buttonToBack.setOnClickListener { tentarSair() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { tentarSair() }
        })

        buttonToggleLimitesCartoes.rotation = if (limitesExpandido) 90f else 0f
        headerLimitesCartoes.setOnClickListener { alternarLimitesExpandido() }

        adapter = AvisosChatAdapter(
            onConfirmar = { secao, mensagem -> confirmar(secao, mensagem) },
            onSolicitarDesconfirmar = { secao, mensagem -> confirmarDesmarcar(secao, mensagem) },
            onOptionsClick = { secao -> mostrarOpcoes(secao) },
            onAvisoCartaoClick = { secao -> mostrarAvisoCartaoAlterado(secao) }
        )
        recyclerChat.layoutManager = LinearLayoutManager(this)
        recyclerChat.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        carregarSecoes()
        carregarSaldoAtual()
    }

    private fun carregarSaldoAtual() {
        bankRepository.loadBank(
            bankId = bankId,
            onSuccess = { bank ->
                saldoProjetado = bank.debit
                saldoCarregado = true
                atualizarTextoSaldo()
            },
            onError = {}
        )
    }

    private fun atualizarTextoSaldo() {
        textSaldoProjetado.text = "Saldo: ${currencyFormat.format(saldoProjetado)}"
        textSaldoProjetado.setTextColor(
            getColor(if (saldoProjetado < 0.0) R.color.alert_red else R.color.white)
        )
    }

    private fun alternarLimitesExpandido() {
        limitesExpandido = !limitesExpandido
        containerLimitesCartoes.visibility = if (limitesExpandido) View.VISIBLE else View.GONE
        buttonToggleLimitesCartoes.rotation = if (limitesExpandido) 90f else 0f
    }

    private fun carregarSecoes() {
        repository.carregarSecoes(
            onSuccess = { todas ->
                val doBanco = todas.filter { it.bankId == bankId }.sortedBy { it.ordenacao }
                exibirSecoes(doBanco)
                marcarComoVisto(doBanco)
                carregarLimitesCartoes(doBanco)
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirSecoes(secoes: List<AvisoSecao>) {
        adapter.atualizarLista(secoes)
        val vazio = secoes.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerChat.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun marcarComoVisto(secoes: List<AvisoSecao>) {
        repository.marcarBancoComoVisto(secoes, onSuccess = {}, onError = {})
    }

    private fun carregarLimitesCartoes(secoes: List<AvisoSecao>) {
        val cardIdsVisiveis = secoes
            .filter { it.tipo == SimulationEntryType.PURCHASE && it.entry.paymentMethod == PaymentMethod.CREDIT }
            .map { it.entry.cardId }
            .toSet()
        if (cardIdsVisiveis.isEmpty()) {
            limitesPorCartao.clear()
            exibirContainerLimites(emptyList())
            return
        }
        bankRepository.loadBank(
            bankId = bankId,
            onSuccess = { bank ->
                entryRepository.loadActiveCreditPurchases { compras ->
                    val novosLimites = bank.cards
                        .filter { it.id in cardIdsVisiveis }
                        .map { card ->
                            val comprasDoCartao = compras.filter { it.cardId == card.id }
                            val disponivel = SimulationCalculator.calcularLimiteDisponivel(card.limit, comprasDoCartao, card.usedAmount)
                            LimiteCartaoProjetado(card.id, card.label, disponivel)
                        }
                    limitesPorCartao.clear()
                    novosLimites.forEach { limitesPorCartao[it.cardId] = it }
                    exibirContainerLimites(novosLimites)
                }
            },
            onError = {}
        )
    }

    private fun exibirContainerLimites(limites: List<LimiteCartaoProjetado>) {
        val visivel = limites.isNotEmpty()
        headerLimitesCartoes.visibility = if (visivel) View.VISIBLE else View.GONE
        containerLimitesCartoes.visibility = if (visivel && limitesExpandido) View.VISIBLE else View.GONE
        containerLimitesCartoes.removeAllViews()
        linhasLimiteCartao.clear()
        limites.sortedBy { it.cardLabel }.forEach { limite ->
            val linha = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.START
                textSize = 14f
                setPadding(0, 4, 0, 4)
            }
            containerLimitesCartoes.addView(linha)
            linhasLimiteCartao[limite.cardId] = linha
            atualizarTextoLimite(limite.cardId)
        }
    }

    private fun atualizarTextoLimite(cardId: String) {
        val limite = limitesPorCartao[cardId] ?: return
        val linha = linhasLimiteCartao[cardId] ?: return
        linha.text = "Limite ${limite.cardLabel}: ${currencyFormat.format(limite.limiteProjetado)}"
        linha.setTextColor(getColor(if (limite.limiteProjetado < 0.0) R.color.alert_red else R.color.white))
    }

    private fun confirmar(secao: AvisoSecao, mensagem: AvisoMensagem) {
        adapter.marcarEmProgresso(secao, mensagem)
        val onErro = { message: String ->
            adapter.desfazerProgresso(secao, mensagem)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        when (secao.tipo) {
            SimulationEntryType.SAVINGS -> repository.alternarPeriodoEconomia(
                entry = secao.entry,
                periodo = mensagem.periodo,
                confirmar = true,
                onSuccess = {
                    adapter.desfazerProgresso(secao, mensagem)
                    registrarAcao(secao, mensagem, confirmarAplicado = true)
                    carregarSecoes()
                },
                onError = onErro
            )
            SimulationEntryType.PURCHASE -> repository.alternarCicloCompra(
                entry = secao.entry,
                periodo = mensagem.periodo,
                confirmar = true,
                onSuccess = {
                    adapter.desfazerProgresso(secao, mensagem)
                    registrarAcao(secao, mensagem, confirmarAplicado = true)
                    carregarSecoes()
                },
                onError = onErro
            )
        }
    }

    private fun valorDaSecao(secao: AvisoSecao): Double = when (secao.tipo) {
        SimulationEntryType.SAVINGS -> secao.entry.monthlyAmount
        SimulationEntryType.PURCHASE -> secao.entry.installmentValue
    }

    private fun registrarAcao(secao: AvisoSecao, mensagem: AvisoMensagem, confirmarAplicado: Boolean) {
        historicoDeAcoes.add(AcaoRealizada(secao, mensagem, confirmarAplicado))
        val sinal = if (confirmarAplicado) -1.0 else 1.0
        val valor = valorDaSecao(secao)
        saldoProjetado += valor * sinal
        atualizarTextoSaldo()
        if (secao.tipo == SimulationEntryType.PURCHASE && secao.entry.paymentMethod == PaymentMethod.CREDIT) {
            val cardId = secao.entry.cardId
            limitesPorCartao[cardId]?.let {
                it.limiteProjetado += valor * -sinal
                atualizarTextoLimite(cardId)
            }
        }
    }

    private fun confirmarDesmarcar(secao: AvisoSecao, mensagem: AvisoMensagem) {
        val mesFormatado = mensagem.periodo.format(monthFormat)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        val valor = valorDaSecao(secao)
        AlertDialog.Builder(this)
            .setTitle("Desfazer confirmação")
            .setMessage(
                "Desfazer a confirmação de $mesFormatado de \"${secao.titulo}\"? " +
                    "Isso devolve ${currencyFormat.format(valor)} pro saldo de ${secao.bankName}."
            )
            .setPositiveButton("Desfazer") { _, _ -> desmarcar(secao, mensagem) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun desmarcar(secao: AvisoSecao, mensagem: AvisoMensagem) {
        adapter.marcarEmProgresso(secao, mensagem)
        val onErro = { message: String ->
            adapter.desfazerProgresso(secao, mensagem)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        when (secao.tipo) {
            SimulationEntryType.SAVINGS -> repository.alternarPeriodoEconomia(
                entry = secao.entry,
                periodo = mensagem.periodo,
                confirmar = false,
                onSuccess = {
                    adapter.desfazerProgresso(secao, mensagem)
                    registrarAcao(secao, mensagem, confirmarAplicado = false)
                    carregarSecoes()
                },
                onError = onErro
            )
            SimulationEntryType.PURCHASE -> repository.alternarCicloCompra(
                entry = secao.entry,
                periodo = mensagem.periodo,
                confirmar = false,
                onSuccess = {
                    adapter.desfazerProgresso(secao, mensagem)
                    registrarAcao(secao, mensagem, confirmarAplicado = false)
                    carregarSecoes()
                },
                onError = onErro
            )
        }
    }

    private fun tentarSair() {
        if (!saldoCarregado || historicoDeAcoes.isEmpty()) {
            finish()
            return
        }
        verificarPendenciasESair()
    }

    private fun verificarPendenciasESair() {
        if (saldoProjetado < 0.0) {
            perguntarSaldoInsuficiente()
            return
        }
        val cartaoNegativo = limitesPorCartao.values.firstOrNull { it.limiteProjetado < 0.0 }
        if (cartaoNegativo != null) {
            perguntarLimiteInsuficiente(cartaoNegativo)
            return
        }
        finish()
    }

    private fun perguntarSaldoInsuficiente() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Ex: 180,00"
        }
        AlertDialog.Builder(this)
            .setTitle("Saldo insuficiente")
            .setMessage(
                "O saldo atual de \"$bankName\" (${currencyFormat.format(saldoProjetado)}) ficou " +
                    "negativo com essas confirmações. Qual é o seu saldo atual de verdade?"
            )
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Atualizar") { _, _ ->
                val novoSaldo = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoSaldo == null) {
                    Toast.makeText(this, "Informe um saldo válido.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                bankRepository.updateBankDebit(
                    bankId = bankId,
                    novoValor = novoSaldo,
                    onSuccess = {
                        saldoProjetado = novoSaldo
                        verificarPendenciasESair()
                    },
                    onError = { erro ->
                        Toast.makeText(this, "Não foi possível atualizar o saldo: $erro", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Fechar") { _, _ -> confirmarDescarte() }
            .show()
    }

    private fun perguntarLimiteInsuficiente(cartao: LimiteCartaoProjetado) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Ex: 180,00"
        }
        AlertDialog.Builder(this)
            .setTitle("Limite insuficiente")
            .setMessage(
                "O limite disponível do cartão \"${cartao.cardLabel}\" " +
                    "(${currencyFormat.format(cartao.limiteProjetado)}) ficou negativo com essas " +
                    "confirmações. Qual é o limite disponível real desse cartão agora?"
            )
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Atualizar") { _, _ ->
                val novoLimite = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoLimite == null) {
                    Toast.makeText(this, "Informe um limite válido.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                aplicarCorrecaoDeLimite(cartao, novoLimite)
            }
            .setNegativeButton("Fechar") { _, _ -> confirmarDescarte() }
            .show()
    }

    private fun aplicarCorrecaoDeLimite(cartao: LimiteCartaoProjetado, novoLimiteReal: Double) {
        val diferenca = cartao.limiteProjetado - novoLimiteReal
        bankRepository.loadBank(
            bankId = bankId,
            onSuccess = { bank ->
                val cardAtual = bank.cards.firstOrNull { it.id == cartao.cardId }
                val usedAmountAtual = cardAtual?.usedAmount ?: 0.0
                bankRepository.atualizarLimiteUsadoCartao(
                    bankId = bankId,
                    cardId = cartao.cardId,
                    novoUsedAmount = usedAmountAtual + diferenca,
                    onSuccess = {
                        cartao.limiteProjetado = novoLimiteReal
                        atualizarTextoLimite(cartao.cardId)
                        verificarPendenciasESair()
                    },
                    onError = { erro ->
                        Toast.makeText(this, "Não foi possível atualizar o limite: $erro", Toast.LENGTH_LONG).show()
                    }
                )
            },
            onError = { erro ->
                Toast.makeText(this, "Não foi possível atualizar o limite: $erro", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun confirmarDescarte() {
        AlertDialog.Builder(this)
            .setTitle("Descartar confirmações?")
            .setMessage(
                "As confirmações feitas agora nesse chat não vão ser salvas — as pendências " +
                    "voltam a aparecer como antes de você entrar. Descartar mesmo assim?"
            )
            .setPositiveButton("Descartar") { _, _ -> desfazerHistoricoEFechar() }
            .setNegativeButton("Voltar", null)
            .show()
    }

    private fun desfazerHistoricoEFechar() {
        desfazerProxima()
    }

    private fun desfazerProxima() {
        if (historicoDeAcoes.isEmpty()) {
            finish()
            return
        }
        val acao = historicoDeAcoes.removeAt(historicoDeAcoes.lastIndex)
        val confirmarParaDesfazer = !acao.confirmarAplicado
        when (acao.secao.tipo) {
            SimulationEntryType.SAVINGS -> repository.alternarPeriodoEconomia(
                entry = acao.secao.entry,
                periodo = acao.mensagem.periodo,
                confirmar = confirmarParaDesfazer,
                onSuccess = { desfazerProxima() },
                onError = { desfazerProxima() }
            )
            SimulationEntryType.PURCHASE -> repository.alternarCicloCompra(
                entry = acao.secao.entry,
                periodo = acao.mensagem.periodo,
                confirmar = confirmarParaDesfazer,
                onSuccess = { desfazerProxima() },
                onError = { desfazerProxima() }
            )
        }
    }

    private fun mostrarOpcoes(secao: AvisoSecao) {
        AlertDialog.Builder(this)
            .setTitle(secao.titulo)
            .setItems(arrayOf("Excluir")) { _, which ->
                if (which == 0) confirmarExclusao(secao)
            }
            .show()
    }

    private fun confirmarExclusao(secao: AvisoSecao) {
        AlertDialog.Builder(this)
            .setTitle("Excluir lançamento")
            .setMessage(
                "Isso exclui \"${secao.titulo}\" por completo — some do módulo de Avisos E da " +
                    "simulação (não aparece mais no canvas). Essa ação não pode ser desfeita. Excluir mesmo assim?"
            )
            .setPositiveButton("Excluir") { _, _ ->
                repository.excluirNo(
                    entry = secao.entry,
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "\"${secao.titulo}\" excluído do módulo de Avisos e da simulação",
                            Toast.LENGTH_SHORT
                        ).show()
                        carregarSecoes()
                    },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarAvisoCartaoAlterado(secao: AvisoSecao) {
        AlertDialog.Builder(this)
            .setTitle("Fechamento do cartão foi alterado")
            .setMessage(
                "A compra \"${secao.titulo}\" foi feita quando o cartão \"${secao.entry.cardLabel}\" " +
                    "fechava dia ${secao.entry.cardClosingDay}. Hoje esse cartão fecha dia " +
                    "${secao.cardFechamentoAtual}.\n\n" +
                    "Os ciclos e parcelas mostrados aqui continuam calculados com o dia " +
                    "${secao.entry.cardClosingDay} (o que valia no momento da compra), pra não " +
                    "bagunçar parcelas que já estavam em andamento. Isso é só um aviso — nada foi " +
                    "alterado automaticamente nessa compra."
            )
            .setPositiveButton("Entendi", null)
            .show()
    }
}
