package com.luamuniz.dinlux.simulation

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.core.MaxHeightRecyclerView
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tela do "canvas" de uma simulaçã
 *
 * Canvas com zoom/pan (via ZoomPanCanvasView), painel de cartões cadastrados no
 * canto superior direito (com botão de minimizar) e botão de criar lançamento, que
 * abre InsertSimulationEntry já com o tipo (compra/economia) escolhido. Os Grupos e
 * lançamentos da simulação são carregados e desenhados no canvas por
 * SimulationCanvasRenderer; tocar num nó mostra as características do lançamento
 */
class SimulationDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SIMULATION_ID = "simulationId"
        const val EXTRA_SIMULATION_TITLE = "simulationTitle"
        const val EXTRA_SIMULATION_ACTIVE = "simulationActive"
    }

    private val bankRepository = BankRepository()
    private val entryRepository = SimulationEntryRepository()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val monthFormat = DateTimeFormatter.ofPattern("MM/yyyy")
    private lateinit var canvasRenderer: SimulationCanvasRenderer
    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonCardsMenu: ImageButton
    private lateinit var buttonMinimizeCardsPanel: ImageButton
    private lateinit var buttonAddNode: ImageButton
    private lateinit var title: TextView
    private lateinit var panelCards: View
    private lateinit var textCardsPanelEmpty: TextView
    private lateinit var recyclerCardsPanel: MaxHeightRecyclerView
    private lateinit var canvasSimulation: ZoomPanCanvasView
    private lateinit var textCanvasEmptyState: TextView
    private lateinit var simulationId: String
    private var simulationActive: Boolean = false
    private lateinit var cardsMenuAdapter: CardsMenuAdapter
    private var bancosPorId: Map<String, Bank> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_simulation_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        simulationId = intent.getStringExtra(EXTRA_SIMULATION_ID) ?: ""
        simulationActive = intent.getBooleanExtra(EXTRA_SIMULATION_ACTIVE, false)

        startComponents()
        title.text = intent.getStringExtra(EXTRA_SIMULATION_TITLE) ?: "Simulação"

        buttonToBack.setOnClickListener { finish() }
        buttonCardsMenu.setOnClickListener { abrirPainelCartoes() }
        buttonMinimizeCardsPanel.setOnClickListener { panelCards.visibility = View.GONE }
        buttonAddNode.setOnClickListener { mostrarEscolhaTipoNo() }
    }

    override fun onResume() {
        super.onResume()
        carregarCanvas()
    }

    private fun startComponents() {
        buttonToBack = findViewById(R.id.button_toBack)
        buttonCardsMenu = findViewById(R.id.button_cards_menu)
        buttonMinimizeCardsPanel = findViewById(R.id.button_minimize_cards_panel)
        buttonAddNode = findViewById(R.id.button_add_node)
        title = findViewById(R.id.title_simulation_detail)
        panelCards = findViewById(R.id.panel_cards)
        textCardsPanelEmpty = findViewById(R.id.text_cards_panel_empty)
        recyclerCardsPanel = findViewById(R.id.recycler_cards_panel)
        canvasSimulation = findViewById(R.id.canvas_simulation)
        textCanvasEmptyState = findViewById(R.id.text_canvas_empty_state)
        recyclerCardsPanel.maxHeightPx = (240 * resources.displayMetrics.density).toInt()

        cardsMenuAdapter = CardsMenuAdapter()
        recyclerCardsPanel.layoutManager = LinearLayoutManager(this)
        recyclerCardsPanel.adapter = cardsMenuAdapter

        canvasRenderer = SimulationCanvasRenderer(this)
    }

    private fun carregarCanvas() {
        // Bancos carregados junto pra sempre mostrar o nome atual de cada banco no painel de detalhes do nó, mesmo que ele
        // tenha sido renomeado depois que o lançamento foi criado
        bankRepository.loadBanks(
            onSuccess = { banks ->
                bancosPorId = banks.associateBy { it.id }
                entryRepository.loadGroups(simulationId,
                    onSuccess = { groups ->
                        entryRepository.loadEntries(simulationId,
                            onSuccess = { entries -> exibirCanvas(groups, entries) },
                            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                        )
                    },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun nomeBancoAtual(entry: SimulationEntry): String =
        bancosPorId[entry.bankId]?.name ?: "Banco excluído"

    private fun exibirCanvas(groups: List<SimulationGroup>, entries: List<SimulationEntry>) {
        textCanvasEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        canvasRenderer.render(
            canvasSimulation.content,
            groups,
            entries,
            onNodeClick = { entry -> mostrarDetalhesNo(entry) },
            onNodeMoved = { entry, destino -> moverNo(entry, destino) },
            onGroupClick = { grupo -> mostrarDialogRenomearGrupo(grupo) }
        )
    }

    // Nome automático é só um ponto de partida
    private fun mostrarDialogRenomearGrupo(grupo: SimulationGroup) {
        val input = EditText(this).apply { setText(grupo.name) }
        AlertDialog.Builder(this)
            .setTitle("Renomear Grupo")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = input.text.toString().trim()
                if (novoNome.isEmpty()) {
                    Toast.makeText(this, "Informe um nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                entryRepository.renameGroup(
                    groupId = grupo.id,
                    novoNome = novoNome,
                    onSuccess = { carregarCanvas() },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun moverNo(entry: SimulationEntry, destino: SimulationGroup) {
        entryRepository.moveEntryToGroup(
            entry = entry,
            toGroup = destino,
            onSuccess = {
                Toast.makeText(this, "\"${entry.title}\" movido para ${destino.name}", Toast.LENGTH_SHORT).show()
                carregarCanvas()
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun formatarData(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)
    }

    // Progresso real (guardado/meta ou parcelas pagas/total) só existe pra nós de
    // simulação ativa
    private fun progressoDoNo(entry: SimulationEntry): Pair<String, Int>? {
        if (!simulationActive) return null
        return when {
            entry.type == SimulationEntryType.SAVINGS -> {
                val percentual = if (entry.targetValue > 0) {
                    ((entry.savedAmount / entry.targetValue) * 100).toInt().coerceIn(0, 100)
                } else 0
                val label = "${currencyFormat.format(entry.savedAmount)} de " +
                    "${currencyFormat.format(entry.targetValue)} guardado ($percentual%)"
                label to percentual
            }
            entry.type == SimulationEntryType.PURCHASE && entry.paymentMethod == PaymentMethod.CREDIT -> {
                val percentual = if (entry.installments > 0) {
                    ((entry.paidInstallments.toDouble() / entry.installments) * 100).toInt().coerceIn(0, 100)
                } else 0
                val label = "${entry.paidInstallments} de ${entry.installments} parcelas pagas ($percentual%)"
                label to percentual
            }
            else -> null
        }
    }

    private fun mostrarDetalhesNo(entry: SimulationEntry) {
        val mensagem = if (entry.type == SimulationEntryType.SAVINGS) {
            "Meta: ${currencyFormat.format(entry.targetValue)}\n" +
                "Valor mensal: ${currencyFormat.format(entry.monthlyAmount)}\n" +
                "Banco: ${nomeBancoAtual(entry)}\n" +
                "Início: ${formatarData(entry.startDate)}\n" +
                "Fim: ${formatarData(entry.endDate)}"
        } else {
            val metodo = if (entry.paymentMethod == PaymentMethod.CREDIT) {
                "Crédito — ${entry.cardLabel}"
            } else {
                "Débito — ${nomeBancoAtual(entry)}"
            }
            val mesInicio = SimulationCalculator.calcularMesInicioCompra(
                entry.createdAt, entry.paymentMethod ?: PaymentMethod.DEBIT, entry.cardClosingDay
            )
            val mesFim = SimulationCalculator.calcularMesFimCompra(mesInicio, entry.installments)
            val linhaBanco = if (entry.paymentMethod == PaymentMethod.CREDIT) {
                "Banco: ${nomeBancoAtual(entry)}\n"
            } else {
                ""
            }
            "Valor total: ${currencyFormat.format(entry.totalValue)}\n" +
                "Forma: $metodo\n" +
                linhaBanco +
                "Parcelas: ${entry.installments}x de ${currencyFormat.format(entry.installmentValue)}\n" +
                "Total com juros: ${currencyFormat.format(entry.totalWithInterest)}\n" +
                "Início: ${mesInicio.format(monthFormat)}\n" +
                "Fim: ${mesFim.format(monthFormat)}"
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_node_details, null)
        view.findViewById<TextView>(R.id.text_node_info).text = mensagem

        val progresso = progressoDoNo(entry)
        val layoutProgresso = view.findViewById<LinearLayout>(R.id.layout_progress_node)
        if (progresso != null) {
            val (label, percentual) = progresso
            view.findViewById<TextView>(R.id.text_node_progress_label).text = label
            view.findViewById<ProgressBar>(R.id.progress_node).progress = percentual
            layoutProgresso.visibility = View.VISIBLE
        } else {
            layoutProgresso.visibility = View.GONE
        }

        AlertDialog.Builder(this)
            .setTitle(entry.title)
            .setView(view)
            .setPositiveButton("Fechar", null)
            .setNeutralButton("Editar") { _, _ -> abrirEdicaoNo(entry) }
            .setNegativeButton("Excluir") { _, _ -> confirmarExclusaoNo(entry) }
            .show()
    }

    private fun abrirEdicaoNo(entry: SimulationEntry) {
        if (entry.type == SimulationEntryType.SAVINGS) {
            mostrarDialogEditarEconomia(entry)
        } else {
            mostrarDialogEditarNome(entry)
        }
    }

    // Compra: valor e parcelas ficam fixos, só o nome pode mudar
    private fun mostrarDialogEditarNome(entry: SimulationEntry) {
        val input = EditText(this).apply { setText(entry.title) }
        AlertDialog.Builder(this)
            .setTitle("Renomear lançamento")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = input.text.toString().trim()
                if (novoNome.isEmpty()) {
                    Toast.makeText(this, "Informe um nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                entryRepository.updateEntryTitle(
                    entry = entry,
                    novoTitulo = novoNome,
                    onSuccess = { carregarCanvas() },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Economia: nome, meta e período (início/fim) podem ser alterados a qualquer
    // momento (o valor a guardar pode mudar de mês pra mês, e o período planejado
    // também); o valor mensal é recalculado com base no tempo restante (hoje até a
    // nova data de fim)
    private fun mostrarDialogEditarEconomia(entry: SimulationEntry) {
        val zoneId = ZoneId.systemDefault()
        var novoInicio = Instant.ofEpochMilli(entry.startDate).atZone(zoneId).toLocalDate()
        var novoFim = Instant.ofEpochMilli(entry.endDate).atZone(zoneId).toLocalDate()

        val inputNome = EditText(this).apply {
            setText(entry.title)
            hint = "Nome"
        }
        val inputMeta = EditText(this).apply {
            setText(String.format(Locale("pt", "BR"), "%.2f", entry.targetValue))
            hint = "Meta (R$)"
        }
        val textInicio = TextView(this).apply {
            text = "Data de início: ${novoInicio.format(dateFormat)}"
        }
        val textFim = TextView(this).apply {
            text = "Data de fim: ${novoFim.format(dateFormat)}"
        }

        textInicio.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    novoInicio = LocalDate.of(year, month + 1, dayOfMonth)
                    textInicio.text = "Data de início: ${novoInicio.format(dateFormat)}"
                },
                novoInicio.year, novoInicio.monthValue - 1, novoInicio.dayOfMonth
            ).show()
        }
        textFim.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    novoFim = LocalDate.of(year, month + 1, dayOfMonth)
                    textFim.text = "Data de fim: ${novoFim.format(dateFormat)}"
                },
                novoFim.year, novoFim.monthValue - 1, novoFim.dayOfMonth
            ).show()
        }

        val margemPx = (12 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val paddingPx = (16 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, 0)
            addView(inputNome)
            addView(inputMeta)
            addView(textInicio, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = margemPx })
            addView(textFim, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = margemPx })
        }

        AlertDialog.Builder(this)
            .setTitle("Editar economia")
            .setView(container)
            .setPositiveButton("Salvar") { _, _ ->
                val novoNome = inputNome.text.toString().trim()
                val novaMeta = inputMeta.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoNome.isEmpty()) {
                    Toast.makeText(this, "Informe um nome", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (novaMeta == null || novaMeta <= 0.0) {
                    Toast.makeText(this, "Informe um valor de meta válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (!novoFim.isAfter(novoInicio)) {
                    Toast.makeText(this, "A data de fim precisa ser depois da data de início", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                salvarEdicaoEconomia(entry, novoNome, novaMeta, novoInicio, novoFim)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarEdicaoEconomia(
        entry: SimulationEntry,
        novoNome: String,
        novaMeta: Double,
        novoInicio: LocalDate,
        novoFim: LocalDate
    ) {
        val zoneId = ZoneId.systemDefault()
        entryRepository.updateSavingsGoal(
            entry = entry,
            novaMeta = novaMeta,
            novoInicio = novoInicio.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            novoFim = novoFim.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            onSuccess = {
                if (novoNome != entry.title) {
                    entryRepository.updateEntryTitle(
                        entry = entry,
                        novoTitulo = novoNome,
                        onSuccess = { carregarCanvas() },
                        onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    carregarCanvas()
                }
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun confirmarExclusaoNo(entry: SimulationEntry) {
        AlertDialog.Builder(this)
            .setTitle("Excluir lançamento")
            .setMessage("Tem certeza que deseja excluir \"${entry.title}\"? Essa ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                entryRepository.deleteEntry(
                    entry = entry,
                    onSuccess = { carregarCanvas() },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirPainelCartoes() {
        panelCards.visibility = View.VISIBLE
        carregarCartoes()
    }

    private fun carregarCartoes() {
        bankRepository.loadBanks(
            onSuccess = { banks -> exibirCartoes(banks) },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun exibirCartoes(banks: List<Bank>) {
        // Cada linha aqui já representa a mesma chave que vai definir os Grupos de nós:
        // um banco (débito/economia) ou um banco+cartão (crédito). A cor mostrada vem
        // do id de cada um (EntityColorPalette), estável em qualquer tela do app
        val linhas = banks.flatMap { bank ->
            val debito = CardMenuRow(id = bank.id, label = "${bank.name} — Débito")
            val cartoes = bank.cards.map { card ->
                CardMenuRow(id = card.id, label = "${bank.name} — ${card.label}")
            }
            listOf(debito) + cartoes
        }
        cardsMenuAdapter.atualizarLista(linhas)
        textCardsPanelEmpty.visibility = if (linhas.isEmpty()) View.VISIBLE else View.GONE
        recyclerCardsPanel.visibility = if (linhas.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun mostrarEscolhaTipoNo() {
        AlertDialog.Builder(this)
            .setTitle("Novo lançamento")
            .setItems(arrayOf("Compra", "Economia")) { _, which ->
                val tipo = if (which == 0) SimulationEntryType.PURCHASE else SimulationEntryType.SAVINGS
                abrirFormularioLancamento(tipo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirFormularioLancamento(tipo: SimulationEntryType) {
        val intent = Intent(this, InsertSimulationEntry::class.java)
        intent.putExtra(InsertSimulationEntry.EXTRA_SIMULATION_ID, simulationId)
        intent.putExtra(InsertSimulationEntry.EXTRA_ENTRY_TYPE, tipo.name)
        startActivity(intent)
    }
}
