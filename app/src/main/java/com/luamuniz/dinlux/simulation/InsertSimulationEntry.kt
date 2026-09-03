package com.luamuniz.dinlux.simulation

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.finance.Card
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formulário de criação de um lançamento (compra ou economia) dentro de uma simulação
 */
class InsertSimulationEntry : AppCompatActivity() {

    companion object {
        const val EXTRA_SIMULATION_ID = "simulationId"
        const val EXTRA_ENTRY_TYPE = "entryType"
    }

    private val bankRepository = BankRepository()
    private val entryRepository = SimulationEntryRepository()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private lateinit var buttonToBack: ImageButton
    private lateinit var title: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var editEntryTitle: EditText

    // Compra
    private lateinit var containerPurchase: LinearLayout
    private lateinit var editPurchaseValue: EditText
    private lateinit var spinnerPurchaseBank: Spinner
    private lateinit var radioPaymentMethod: RadioGroup
    private lateinit var containerPurchaseCredit: LinearLayout
    private lateinit var spinnerPurchaseCard: Spinner
    private lateinit var textPurchaseInterestInfo: TextView
    private lateinit var editPurchaseInstallments: EditText
    private lateinit var textPurchasePreview: TextView

    // Economia
    private lateinit var containerSavings: LinearLayout
    private lateinit var editSavingsTarget: EditText
    private lateinit var spinnerSavingsBank: Spinner
    private lateinit var textSavingsStartDate: TextView
    private lateinit var textSavingsEndDate: TextView
    private lateinit var textSavingsPreview: TextView

    private lateinit var buttonSaveEntry: Button
    private lateinit var simulationId: String
    private lateinit var entryType: SimulationEntryType
    private var banks: List<Bank> = emptyList()
    private var selectedPurchaseBank: Bank? = null
    private var selectedPurchaseCard: Card? = null
    private var selectedSavingsBank: Bank? = null
    private var savingsStartDate: LocalDate? = null
    private var savingsEndDate: LocalDate? = null
    private var comprasCreditoAtivas: List<SimulationEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insert_simulation_entry)

        simulationId = intent.getStringExtra(EXTRA_SIMULATION_ID) ?: ""
        entryType = SimulationEntryType.valueOf(
            intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: SimulationEntryType.PURCHASE.name
        )

        startComponents()

        // Ajusta o padding inferior do ScrollView conforme o teclado (ime) e as barras do
        // sistema, senão o campo em edição fica escondido atrás do teclado
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            scrollView.setPadding(0, 0, 0, maxOf(keyboard.bottom, systemBars.bottom))
            insets
        }
        configurarTipo()
        configurarListeners()
        carregarBancos()
        carregarComprasCreditoAtivas()
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        title = findViewById(R.id.title_insert_entry)
        scrollView = findViewById(R.id.scrollView)
        editEntryTitle = findViewById(R.id.edit_entry_title)

        containerPurchase = findViewById(R.id.container_purchase)
        editPurchaseValue = findViewById(R.id.edit_purchase_value)
        spinnerPurchaseBank = findViewById(R.id.spinner_purchase_bank)
        radioPaymentMethod = findViewById(R.id.radio_purchase_payment_method)
        containerPurchaseCredit = findViewById(R.id.container_purchase_credit)
        spinnerPurchaseCard = findViewById(R.id.spinner_purchase_card)
        textPurchaseInterestInfo = findViewById(R.id.text_purchase_interest_info)
        editPurchaseInstallments = findViewById(R.id.edit_purchase_installments)
        textPurchasePreview = findViewById(R.id.text_purchase_preview)

        containerSavings = findViewById(R.id.container_savings)
        editSavingsTarget = findViewById(R.id.edit_savings_target)
        spinnerSavingsBank = findViewById(R.id.spinner_savings_bank)
        textSavingsStartDate = findViewById(R.id.text_savings_start_date)
        textSavingsEndDate = findViewById(R.id.text_savings_end_date)
        textSavingsPreview = findViewById(R.id.text_savings_preview)

        buttonSaveEntry = findViewById(R.id.button_save_entry)
    }

    private fun configurarTipo() {
        val isPurchase = entryType == SimulationEntryType.PURCHASE
        title.text = if (isPurchase) "Nova Compra" else "Nova Economia"
        containerPurchase.visibility = if (isPurchase) View.VISIBLE else View.GONE
        containerSavings.visibility = if (isPurchase) View.GONE else View.VISIBLE
    }

    private fun configurarListeners() {
        buttonToBack.setOnClickListener { finish() }

        radioPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            val isCredito = checkedId == R.id.radio_purchase_credit
            containerPurchaseCredit.visibility = if (isCredito) View.VISIBLE else View.GONE
            atualizarPreviewCompra()
        }

        spinnerPurchaseBank.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPurchaseBank = banks.getOrNull(position)
                popularSpinnerCartoes()
                atualizarPreviewCompra()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerPurchaseCard.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPurchaseCard = selectedPurchaseBank?.cards?.getOrNull(position)
                atualizarInfoJuros()
                atualizarPreviewCompra()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerSavingsBank.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSavingsBank = banks.getOrNull(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val purchaseWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { atualizarPreviewCompra() }
        }
        editPurchaseValue.addTextChangedListener(purchaseWatcher)
        editPurchaseInstallments.addTextChangedListener(purchaseWatcher)

        val savingsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { atualizarPreviewEconomia() }
        }
        editSavingsTarget.addTextChangedListener(savingsWatcher)

        textSavingsStartDate.setOnClickListener { abrirSeletorData(isInicio = true) }
        textSavingsEndDate.setOnClickListener { abrirSeletorData(isInicio = false) }

        buttonSaveEntry.setOnClickListener { validarESalvar() }
    }

    // ---------- CARREGAR BANCOS/CARTÕES ----------

    private fun carregarBancos() {
        bankRepository.loadBanks(
            onSuccess = { lista ->
                banks = lista
                if (banks.isEmpty()) {
                    avisarSemBancoEFechar()
                } else {
                    val nomes = banks.map { it.name }
                    spinnerPurchaseBank.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nomes)
                    spinnerSavingsBank.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nomes)
                    selectedPurchaseBank = banks.first()
                    selectedSavingsBank = banks.first()
                    popularSpinnerCartoes()
                }
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    // Carrega em paralelo com carregarBancos() usada só na hora de
    // validar uma nova compra em crédito contra o limite realmente disponível do cartão
    // Falha silenciosa: se não conseguir carregar, a lista fica
    // vazia e a validação cai de volta a comparar só com o limite total do cartão
    private fun carregarComprasCreditoAtivas() {
        entryRepository.loadActiveCreditPurchases { compras -> comprasCreditoAtivas = compras }
    }

    private fun avisarSemBancoEFechar() {
        AlertDialog.Builder(this)
            .setTitle("Nenhum banco cadastrado")
            .setMessage("Cadastre um banco em Finanças antes de criar um lançamento.")
            .setCancelable(false)
            .setPositiveButton("Entendi") { _, _ -> finish() }
            .show()
    }

    private fun popularSpinnerCartoes() {
        val cards = selectedPurchaseBank?.cards ?: emptyList()
        spinnerPurchaseCard.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cards.map { it.label })
        selectedPurchaseCard = cards.firstOrNull()
        atualizarInfoJuros()
    }

    private fun atualizarInfoJuros() {
        val card = selectedPurchaseCard
        textPurchaseInterestInfo.text = if (card != null && card.hasInterest) {
            "Este cartão cobra ${card.interestRate}% de juros ao mês (a partir de 2 parcelas)."
        } else {
            "Este cartão não cobra juros no parcelamento."
        }
    }

    // ---------- DATA (ECONOMIA) ----------

    private fun abrirSeletorData(isInicio: Boolean) {
        val referencia = (if (isInicio) savingsStartDate else savingsEndDate) ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val data = LocalDate.of(year, month + 1, dayOfMonth)
                if (isInicio) {
                    savingsStartDate = data
                    textSavingsStartDate.text = "Data de início: ${data.format(dateFormat)}"
                } else {
                    savingsEndDate = data
                    textSavingsEndDate.text = "Data de fim: ${data.format(dateFormat)}"
                }
                atualizarPreviewEconomia()
            },
            referencia.year, referencia.monthValue - 1, referencia.dayOfMonth
        ).show()
    }

    // ---------- PREVIEW EM TEMPO REAL ----------

    private fun atualizarPreviewCompra() {
        val valorTotal = editPurchaseValue.text.toString().replace(",", ".").toDoubleOrNull()
        if (valorTotal == null || valorTotal <= 0.0) {
            textPurchasePreview.text = ""
            return
        }
        val isCredito = radioPaymentMethod.checkedRadioButtonId == R.id.radio_purchase_credit
        val parcelas = (editPurchaseInstallments.text.toString().toIntOrNull() ?: 1).coerceAtLeast(1)
        val card = selectedPurchaseCard
        val taxa = if (isCredito && parcelas > 1 && card?.hasInterest == true) card.interestRate else 0.0

        val valorParcela = SimulationCalculator.calcularParcela(valorTotal, parcelas, taxa)
        val total = SimulationCalculator.calcularTotalComJuros(valorParcela, parcelas)

        textPurchasePreview.text = when {
            parcelas <= 1 -> "1x de ${currencyFormat.format(valorParcela)} (à vista)"
            taxa > 0.0 -> "${parcelas}x de ${currencyFormat.format(valorParcela)} — total com juros: ${currencyFormat.format(total)}"
            else -> "${parcelas}x de ${currencyFormat.format(valorParcela)} (sem juros)"
        }
    }

    private fun atualizarPreviewEconomia() {
        val meta = editSavingsTarget.text.toString().replace(",", ".").toDoubleOrNull()
        val inicio = savingsStartDate
        val fim = savingsEndDate
        if (meta == null || meta <= 0.0 || inicio == null || fim == null || !fim.isAfter(inicio)) {
            textSavingsPreview.text = ""
            return
        }
        val meses = SimulationCalculator.calcularMesesEntre(inicio, fim)
        val valorMensal = SimulationCalculator.calcularValorMensalEconomia(meta, inicio, fim)
        textSavingsPreview.text = "${currencyFormat.format(valorMensal)} por mês, durante $meses ${if (meses == 1) "mês" else "meses"}."
    }

    // ---------- VALIDAR E SALVAR ----------

    private fun validarESalvar() {
        val titulo = editEntryTitle.text.toString().trim()
        if (titulo.isEmpty()) {
            Toast.makeText(this, "Informe um nome para o lançamento", Toast.LENGTH_SHORT).show()
            return
        }

        if (entryType == SimulationEntryType.PURCHASE) {
            validarESalvarCompra(titulo)
        } else {
            validarESalvarEconomia(titulo)
        }
    }

    private fun validarESalvarCompra(titulo: String) {
        val bank = selectedPurchaseBank
        if (bank == null) {
            Toast.makeText(this, "Selecione um banco", Toast.LENGTH_SHORT).show()
            return
        }
        val valorTotal = editPurchaseValue.text.toString().replace(",", ".").toDoubleOrNull()
        if (valorTotal == null || valorTotal <= 0.0) {
            Toast.makeText(this, "Informe um valor total válido", Toast.LENGTH_SHORT).show()
            return
        }
        val parcelas = editPurchaseInstallments.text.toString().toIntOrNull()
        if (parcelas == null || parcelas <= 0) {
            Toast.makeText(this, "Informe um número de parcelas válido", Toast.LENGTH_SHORT).show()
            return
        }

        val isCredito = radioPaymentMethod.checkedRadioButtonId == R.id.radio_purchase_credit

        if (isCredito) {
            val card = selectedPurchaseCard
            if (card == null) {
                Toast.makeText(this, "Selecione um cartão", Toast.LENGTH_SHORT).show()
                return
            }
            val taxa = if (parcelas > 1 && card.hasInterest) card.interestRate else 0.0
            val valorParcela = SimulationCalculator.calcularParcela(valorTotal, parcelas, taxa)
            val totalComJuros = SimulationCalculator.calcularTotalComJuros(valorParcela, parcelas)

            // Limite realmente disponível do cartão, mesma conta que a tela de
            // Finanças usa (limite cadastrado menos as parcelas em aberto de outras
            // compras já ativas menos o uso manual fora de Simulação). Comparar contra
            // isso, e não só contra o limite total cadastrado, é o que faz o bloqueio
            // valer mesmo quando o cartão já tem outras compras usando o limite
            val comprasDoCartao = comprasCreditoAtivas.filter { it.bankId == bank.id && it.cardId == card.id }
            val disponivel = SimulationCalculator.calcularLimiteDisponivel(card.limit, comprasDoCartao, card.usedAmount)

            if (totalComJuros > disponivel) {
                Toast.makeText(
                    this,
                    "Essa compra (${currencyFormat.format(totalComJuros)} no total) ultrapassa o limite disponível do cartão ${card.label} (${currencyFormat.format(disponivel.coerceAtLeast(0.0))}). Reduza o valor ou o número de parcelas, ou escolha outro cartão.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val entry = SimulationEntry(
                simulationId = simulationId,
                type = SimulationEntryType.PURCHASE,
                title = titulo,
                paymentMethod = PaymentMethod.CREDIT,
                bankId = bank.id,
                bankName = bank.name,
                cardId = card.id,
                cardLabel = card.label,
                cardClosingDay = card.closingDay,
                totalValue = valorTotal,
                installments = parcelas,
                interestRate = taxa,
                installmentValue = valorParcela,
                totalWithInterest = totalComJuros
            )

            salvarEntry(entry)
        } else {
            val valorParcela = SimulationCalculator.calcularParcela(valorTotal, parcelas, 0.0)

            val entry = SimulationEntry(
                simulationId = simulationId,
                type = SimulationEntryType.PURCHASE,
                title = titulo,
                paymentMethod = PaymentMethod.DEBIT,
                bankId = bank.id,
                bankName = bank.name,
                totalValue = valorTotal,
                installments = parcelas,
                interestRate = 0.0,
                installmentValue = valorParcela,
                totalWithInterest = valorTotal
            )

            val aviso = if (valorParcela > bank.debit) {
                "A primeira parcela (${currencyFormat.format(valorParcela)}) ultrapassa o saldo atual de ${bank.name} (${currencyFormat.format(bank.debit)}). Continuar mesmo assim?"
            } else null

            confirmarEEntao(aviso) { salvarEntry(entry) }
        }
    }

    private fun validarESalvarEconomia(titulo: String) {
        val bank = selectedSavingsBank
        if (bank == null) {
            Toast.makeText(this, "Selecione um banco", Toast.LENGTH_SHORT).show()
            return
        }
        val meta = editSavingsTarget.text.toString().replace(",", ".").toDoubleOrNull()
        if (meta == null || meta <= 0.0) {
            Toast.makeText(this, "Informe um valor de meta válido", Toast.LENGTH_SHORT).show()
            return
        }
        val inicio = savingsStartDate
        val fim = savingsEndDate
        if (inicio == null || fim == null) {
            Toast.makeText(this, "Escolha a data de início e de fim", Toast.LENGTH_SHORT).show()
            return
        }
        if (!fim.isAfter(inicio)) {
            Toast.makeText(this, "A data de fim precisa ser depois da data de início", Toast.LENGTH_SHORT).show()
            return
        }

        val valorMensal = SimulationCalculator.calcularValorMensalEconomia(meta, inicio, fim)
        val zoneId = ZoneId.systemDefault()

        val entry = SimulationEntry(
            simulationId = simulationId,
            type = SimulationEntryType.SAVINGS,
            title = titulo,
            paymentMethod = PaymentMethod.DEBIT,
            bankId = bank.id,
            bankName = bank.name,
            targetValue = meta,
            startDate = inicio.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endDate = fim.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            monthlyAmount = valorMensal
        )

        val aviso = if (valorMensal > bank.debit) {
            "O valor mensal calculado (${currencyFormat.format(valorMensal)}) ultrapassa o saldo atual de ${bank.name} (${currencyFormat.format(bank.debit)}). Continuar mesmo assim?"
        } else null

        confirmarEEntao(aviso) { salvarEntry(entry) }
    }

    private fun confirmarEEntao(mensagemAviso: String?, acao: () -> Unit) {
        if (mensagemAviso == null) {
            acao()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Aviso")
            .setMessage(mensagemAviso)
            .setPositiveButton("Continuar mesmo assim") { _, _ -> acao() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarEntry(entry: SimulationEntry) {
        buttonSaveEntry.isEnabled = false
        entryRepository.createEntry(
            entry = entry,
            onSuccess = {
                Toast.makeText(this, "Lançamento salvo com sucesso", Toast.LENGTH_SHORT).show()
                finish()
            },
            onError = { message ->
                buttonSaveEntry.isEnabled = true
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
