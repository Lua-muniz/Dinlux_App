package com.luamuniz.dinlux.simulation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository

/**
 * Lista as simulações do usuário, separadas em Ativas
 * e Inativas (SimulationListAdapter). Segue o mesmo padrão de estado vazio/lista já
 * usado em Finance
 *
 * Tocar numa simulação abre o detalhe dela. O botão de três pontos no canto de cada
 * item abre as opções de renomear/excluir/ativar ou desativar
 */
class SimulationListActivity : AppCompatActivity() {

    private val viewModel: SimulationViewModel by viewModels()
    private val entryRepository = SimulationEntryRepository()
    private val bankRepository = BankRepository()
    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonAddSimulation: ImageButton
    private lateinit var recyclerSimulations: RecyclerView
    private lateinit var textEmptyState: TextView
    private lateinit var adapter: SimulationListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_simulation_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        buttonAddSimulation = findViewById(R.id.button_add_simulation)
        recyclerSimulations = findViewById(R.id.recycler_simulations)
        textEmptyState = findViewById(R.id.text_empty_state)

        adapter = SimulationListAdapter(
            onClick = { simulation -> abrirDetalhe(simulation) },
            onOptionsClick = { simulation -> mostrarOpcoes(simulation) }
        )
        recyclerSimulations.layoutManager = LinearLayoutManager(this)
        recyclerSimulations.adapter = adapter

        buttonToBack.setOnClickListener { finish() }
        buttonAddSimulation.setOnClickListener { mostrarDialogCriarSimulacao() }

        observeUiState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSimulations()
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is SimulationListUiState.Loading -> Unit
                is SimulationListUiState.Loaded -> exibirLista(state.simulations)
                is SimulationListUiState.Error -> Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.actionMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirLista(simulations: List<Simulation>) {
        adapter.atualizarLista(simulations)
        val vazio = simulations.isEmpty()
        textEmptyState.visibility = if (vazio) View.VISIBLE else View.GONE
        recyclerSimulations.visibility = if (vazio) View.GONE else View.VISIBLE
    }

    private fun abrirDetalhe(simulation: Simulation) {
        val intent = Intent(this, SimulationDetailActivity::class.java)
        intent.putExtra(SimulationDetailActivity.EXTRA_SIMULATION_ID, simulation.id)
        intent.putExtra(SimulationDetailActivity.EXTRA_SIMULATION_TITLE, simulation.title)
        intent.putExtra(SimulationDetailActivity.EXTRA_SIMULATION_ACTIVE, simulation.active)
        startActivity(intent)
    }

    private fun mostrarOpcoes(simulation: Simulation) {
        val opcaoAtivar = if (simulation.active) "Desativar" else "Ativar"
        AlertDialog.Builder(this)
            .setTitle(simulation.title)
            .setItems(arrayOf("Renomear", "Excluir", opcaoAtivar)) { _, which ->
                when (which) {
                    0 -> mostrarDialogRenomear(simulation)
                    1 -> confirmarExclusao(simulation)
                    2 -> if (simulation.active) desativarSimulacao(simulation) else prepararAtivacao(simulation)
                }
            }
            .show()
    }

    private fun mostrarDialogCriarSimulacao() {
        val input = EditText(this).apply { hint = "Ex: Comprar Móveis" }
        val checkboxAtiva = CheckBox(this).apply { text = "Criar já como ativa" }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val paddingPx = (16 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, 0)
            addView(input)
            addView(checkboxAtiva)
        }
        AlertDialog.Builder(this)
            .setTitle("Nova simulação")
            .setView(container)
            .setPositiveButton("Criar") { _, _ ->
                val titulo = input.text.toString().trim()
                if (titulo.isNotEmpty()) viewModel.createSimulation(titulo, checkboxAtiva.isChecked)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Desativar não muda nada nos lançamentos, só para de ser acompanhada pelos
    // Avisos e volta a não descontar nada de verdade
    private fun desativarSimulacao(simulation: Simulation) {
        viewModel.setActive(simulation.id, false)
    }

    // Ativar recalcula a data de cada lançamento a partir de hoje (mantendo parcelas/
    // meses/juros)
    private fun prepararAtivacao(simulation: Simulation) {
        entryRepository.loadEntries(
            simulationId = simulation.id,
            onSuccess = { entries ->
                bankRepository.loadBanks(
                    onSuccess = { banks -> confirmarAtivacao(simulation, entries, banks) },
                    onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
                )
            },
            onError = { message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun confirmarAtivacao(simulation: Simulation, entries: List<SimulationEntry>, banks: List<Bank>) {
        val mensagem = if (entries.isEmpty()) {
            "Essa simulação ainda não tem lançamentos. Ativar \"${simulation.title}\"?"
        } else {
            "${entries.size} lançamento(s) terão a data recalculada a partir de hoje, mantendo parcelas, meses e juros. Ativar \"${simulation.title}\"?"
        }
        AlertDialog.Builder(this)
            .setTitle("Ativar simulação")
            .setMessage(mensagem)
            .setPositiveButton("Ativar") { _, _ -> viewModel.activateSimulation(simulation.id, entries, banks) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogRenomear(simulation: Simulation) {
        val input = EditText(this).apply { setText(simulation.title) }
        AlertDialog.Builder(this)
            .setTitle("Renomear simulação")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val novoTitulo = input.text.toString().trim()
                if (novoTitulo.isNotEmpty()) viewModel.renameSimulation(simulation.id, novoTitulo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarExclusao(simulation: Simulation) {
        AlertDialog.Builder(this)
            .setTitle("Excluir simulação")
            .setMessage("Tem certeza que deseja excluir \"${simulation.title}\"? Essa ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ -> viewModel.deleteSimulation(simulation.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
