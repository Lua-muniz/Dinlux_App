package com.luamuniz.dinlux.home

import android.content.Intent
import android.app.DatePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
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
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.luamuniz.dinlux.BuildConfig
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.authentication.AuthRepository
import com.luamuniz.dinlux.authentication.Login
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.core.TermosDePrivacidade
import com.luamuniz.dinlux.core.DebugClock
import com.luamuniz.dinlux.excerpt.StatementTransactionRepository
import com.luamuniz.dinlux.excerpt.StatementTransactionRecord
import com.luamuniz.dinlux.export.ExportDataActivity
import com.luamuniz.dinlux.excerpt.InsertExcerpt
import com.luamuniz.dinlux.graphics.DashboardCardsAdapter
import com.luamuniz.dinlux.graphics.DashboardMovimentacoesAdapter
import com.luamuniz.dinlux.graphics.DashboardSectionAdapter
import com.luamuniz.dinlux.graphics.GraphicsRepository
import com.luamuniz.dinlux.list.ListsActivity
import com.luamuniz.dinlux.notifications.AvisosListActivity
import com.luamuniz.dinlux.notifications.AvisosRepository
import com.luamuniz.dinlux.profile.AlterEmail
import com.luamuniz.dinlux.profile.AlterName
import com.luamuniz.dinlux.profile.AlterPassword
import com.luamuniz.dinlux.simulation.SimulationListActivity
import com.luamuniz.dinlux.tutorial.Tutorial
import java.time.LocalDate
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Home : AppCompatActivity() {
    private lateinit var buttonMenu: ImageButton
    private lateinit var buttonWarning: TextView
    private lateinit var textWarningBadge: TextView
    private lateinit var buttonFinance: TextView
    private lateinit var buttonSimulation: TextView
    private lateinit var buttonList: TextView
    private lateinit var buttonInsertExcerpt: TextView
    private lateinit var buttonTutorial: TextView
    private lateinit var tabSimulacoes: TextView
    private lateinit var tabMovimentacoes: TextView
    private lateinit var tabCartoes: TextView
    private lateinit var recyclerDashboard: RecyclerView
    private lateinit var progressDashboard: ProgressBar
    private val dashboardMovimentacoesAdapter = DashboardMovimentacoesAdapter()
    private val dashboardSectionAdapter = DashboardSectionAdapter()
    private val dashboardCardsAdapter = DashboardCardsAdapter()
    private val graphicsRepository = GraphicsRepository()
    private enum class AbaDashboard { SIMULACOES, MOVIMENTACOES, CARTOES }
    private var abaDashboardSelecionada = AbaDashboard.SIMULACOES
    private val dashboardHandler = Handler(Looper.getMainLooper())
    private var dashboardRequestId = 0
    private val avisosRepository = AvisosRepository()
    private val authRepository = AuthRepository()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var textPanelGreeting: TextView
    private lateinit var buttonPanelClose: ImageButton
    private lateinit var buttonPanelName: MaterialButton
    private lateinit var buttonPanelEmail: MaterialButton
    private lateinit var buttonPanelPassword: MaterialButton
    private lateinit var buttonPanelDeleteAccount: MaterialButton
    private lateinit var buttonPanelPrivacyPolicy: MaterialButton
    private lateinit var buttonPanelExit: MaterialButton
    private lateinit var buttonPanelTestMode: MaterialButton
    private lateinit var buttonPanelExportData: MaterialButton
    private val statementTransactionRepository = StatementTransactionRepository()
    private lateinit var linePanelTestMode: View
    private lateinit var cardBoasVindas: androidx.cardview.widget.CardView
    private lateinit var textBoasVindasTitulo: TextView
    private lateinit var buttonFecharBoasVindas: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startComponents()

        buttonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        buttonWarning.setOnClickListener {
            val intent = Intent(this, AvisosListActivity::class.java)
            startActivity(intent)
        }

        buttonFinance.setOnClickListener {
            val intent = Intent(this, Finance::class.java)
            startActivity(intent)
        }

        buttonSimulation.setOnClickListener {
            val intent = Intent(this, SimulationListActivity::class.java)
            startActivity(intent)
        }

        buttonList.setOnClickListener {
            val intent = Intent(this, ListsActivity::class.java)
            startActivity(intent)
        }

        buttonInsertExcerpt.setOnClickListener {
            val intent = Intent(this, InsertExcerpt::class.java)
            startActivity(intent)
        }

        buttonTutorial.setOnClickListener {
            val intent = Intent(this, Tutorial::class.java)
            startActivity(intent)
        }

        configurarPainelLateral()
        configurarDashboard()
        verificarRetencaoExtrato()
        verificarBoasVindas()
    }

    // Painel lateral de perfil
    private fun configurarPainelLateral() {
        buttonPanelClose.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        buttonPanelName.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AlterName::class.java))
        }

        buttonPanelEmail.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AlterEmail::class.java))
        }

        buttonPanelPassword.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AlterPassword::class.java))
        }

        buttonPanelDeleteAccount.setOnClickListener {
            confirmarExclusaoConta()
        }

        buttonPanelPrivacyPolicy.setOnClickListener {
            mostrarPoliticasDePrivacidade()
        }

        buttonPanelExportData.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ExportDataActivity::class.java))
        }

        buttonPanelExit.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        if (BuildConfig.DEBUG) {
            buttonPanelTestMode.visibility = View.VISIBLE
            linePanelTestMode.visibility = View.VISIBLE
            atualizarTextoModoTeste()
            buttonPanelTestMode.setOnClickListener {
                abrirSeletorDeDataDeTeste()
            }
        }
    }

    private fun atualizarTextoModoTeste() {
        val dataSimulada = DebugClock.dataAtual()
        buttonPanelTestMode.text = if (dataSimulada != null) {
            "Modo Teste (hoje = ${dataSimulada.dayOfMonth}/${dataSimulada.monthValue}/${dataSimulada.year})"
        } else {
            getString(R.string.Modo_Teste)
        }
    }

    private fun abrirSeletorDeDataDeTeste() {
        val hoje = DebugClock.hoje()
        val dialog = DatePickerDialog(
            this,
            { _, ano, mes, dia ->
                DebugClock.definir(LocalDate.of(ano, mes + 1, dia))
                atualizarTextoModoTeste()
                Toast.makeText(this, "Avisos agora considera hoje como $dia/${mes + 1}/$ano", Toast.LENGTH_LONG).show()
            },
            hoje.year,
            hoje.monthValue - 1,
            hoje.dayOfMonth
        )
        dialog.setButton(
            DatePickerDialog.BUTTON_NEUTRAL,
            "Limpar (voltar pra hoje de verdade)"
        ) { _, _ ->
            DebugClock.limpar()
            atualizarTextoModoTeste()
            Toast.makeText(this, "Modo Teste desligado, Avisos volta a considerar a data real", Toast.LENGTH_LONG).show()
        }
        dialog.show()
    }

    // Exclusão de conta
    private fun confirmarExclusaoConta() {
        val padding = (16 * resources.displayMetrics.density).toInt()

        val textoAviso = TextView(this).apply {
            text = "Todos os seus dados (bancos, cartões, simulações, extratos e avisos) " +
                "serão apagados permanentemente. Essa ação não pode ser desfeita.\n\n" +
                "Digite sua senha atual para confirmar:"
            setPadding(padding, padding, padding, 0)
        }
        val inputSenha = EditText(this).apply {
            hint = "Senha atual"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(padding, padding, padding, padding)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(textoAviso)
            addView(inputSenha)
        }

        AlertDialog.Builder(this)
            .setTitle("Excluir conta")
            .setView(container)
            .setPositiveButton("Excluir conta") { _, _ ->
                val senha = inputSenha.text.toString()
                if (senha.isBlank()) {
                    Toast.makeText(this, "Digite sua senha atual", Toast.LENGTH_LONG).show()
                } else {
                    executarExclusaoConta(senha)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executarExclusaoConta(senhaAtual: String) {
        authRepository.deleteAccount(
            currentPassword = senhaAtual,
            onSuccess = {
                Toast.makeText(this, "Conta excluída com sucesso", Toast.LENGTH_LONG).show()
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            },
            onError = { mensagem ->
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Políticas de Privacidade
    private fun mostrarPoliticasDePrivacidade() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            exibirDialogoPoliticas(dataAceite = null, versao = null)
            return
        }
        FirebaseFirestore.getInstance().collection(FirestoreCollections.USERS).document(uid)
            .get()
            .addOnSuccessListener { documento ->
                val timestampAceite = documento?.getLong("termsAcceptedAt")
                val versao = documento?.getString("termsVersion")
                exibirDialogoPoliticas(dataAceite = timestampAceite, versao = versao)
            }
            .addOnFailureListener {
                exibirDialogoPoliticas(dataAceite = null, versao = null)
            }
    }

    private fun exibirDialogoPoliticas(dataAceite: Long?, versao: String?) {
        val formatoData = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        val cabecalho = if (dataAceite != null) {
            "Você aceitou este termo em ${formatoData.format(Date(dataAceite))}" +
                (if (versao != null) " (versão $versao)." else ".")
        } else {
            "Não foi possível confirmar a data exata do seu aceite."
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.Politicas_de_Privacidade))
            .setMessage("$cabecalho\n\n${TermosDePrivacidade.TEXTO}")
            .setPositiveButton("Fechar", null)
            .show()
    }

    // Saudação "Olá, {nome}"
    private fun carregarSaudacaoPainel() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dr: DocumentReference = FirebaseFirestore.getInstance()
            .collection(FirestoreCollections.USERS).document(uid)
        dr.addSnapshotListener { documentSnapshot, _ ->
            val nome = documentSnapshot?.getString("nome").orEmpty()
            textPanelGreeting.text = if (nome.isNotBlank()) "Olá, $nome" else "Olá!"
        }
    }

    // Dashboard de gráficos embutido na Home
    private fun configurarDashboard() {
        selecionarAbaDashboard(AbaDashboard.SIMULACOES)

        tabSimulacoes.setOnClickListener { selecionarAbaDashboard(AbaDashboard.SIMULACOES) }
        tabMovimentacoes.setOnClickListener { selecionarAbaDashboard(AbaDashboard.MOVIMENTACOES) }
        tabCartoes.setOnClickListener { selecionarAbaDashboard(AbaDashboard.CARTOES) }
    }

    private fun selecionarAbaDashboard(aba: AbaDashboard) {
        abaDashboardSelecionada = aba
        destacarAba(tabSimulacoes, aba == AbaDashboard.SIMULACOES)
        destacarAba(tabMovimentacoes, aba == AbaDashboard.MOVIMENTACOES)
        destacarAba(tabCartoes, aba == AbaDashboard.CARTOES)

        recyclerDashboard.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerDashboard.adapter = when (aba) {
            AbaDashboard.SIMULACOES -> dashboardSectionAdapter
            AbaDashboard.MOVIMENTACOES -> dashboardMovimentacoesAdapter
            AbaDashboard.CARTOES -> dashboardCardsAdapter
        }

        carregarAbaAtualDashboard()
    }

    private fun destacarAba(tab: TextView, selecionada: Boolean) {
        tab.setBackgroundResource(if (selecionada) R.drawable.shape_tab_pill else 0)
        tab.alpha = if (selecionada) 1f else 0.6f
    }

    private fun carregarAbaAtualDashboard() {
        val requestId = ++dashboardRequestId
        progressDashboard.visibility = View.VISIBLE
        recyclerDashboard.visibility = View.INVISIBLE

        // Tempo mínimo que o spinner fica visível, mesmo que os dados voltem antes disso
        // Sem isso, com o Firestore em cache local, o carregamento fica rápido demais
        // pra sequer perceber a animação
        var tempoMinimoAtingido = false
        var aoFicarPronto: (() -> Unit)? = null
        dashboardHandler.postDelayed({
            if (requestId != dashboardRequestId) return@postDelayed
            tempoMinimoAtingido = true
            aoFicarPronto?.invoke()
        }, DASHBOARD_LOADING_MIN_MS)

        fun exibirQuandoPronto(aplicar: () -> Unit) {
            if (requestId != dashboardRequestId) return // usuário já trocou de aba de novo
            val revelar = {
                aplicar()
                progressDashboard.visibility = View.GONE
                recyclerDashboard.visibility = View.VISIBLE
            }
            if (tempoMinimoAtingido) revelar() else aoFicarPronto = revelar
        }

        when (abaDashboardSelecionada) {
            AbaDashboard.SIMULACOES -> graphicsRepository.carregarGraficoSimulacoes(
                onSuccess = { secoes -> exibirQuandoPronto { dashboardSectionAdapter.atualizar(secoes) } },
                onError = { exibirQuandoPronto { dashboardSectionAdapter.atualizar(emptyList()) } }
            )
            AbaDashboard.MOVIMENTACOES -> graphicsRepository.carregarGraficoMovimentacoes(
                onSuccess = { cards -> exibirQuandoPronto { dashboardMovimentacoesAdapter.atualizar(cards) } },
                onError = { exibirQuandoPronto { dashboardMovimentacoesAdapter.atualizar(emptyList()) } }
            )
            AbaDashboard.CARTOES -> graphicsRepository.carregarGraficoCartoes(
                onSuccess = { cards -> exibirQuandoPronto { dashboardCardsAdapter.atualizar(cards) } },
                onError = { exibirQuandoPronto { dashboardCardsAdapter.atualizar(emptyList()) } }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        carregarBadgeAvisos()
        carregarAbaAtualDashboard()
    }

    override fun onStart() {
        super.onStart()
        carregarSaudacaoPainel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Evita que um postDelayed do spinner do dashboard dispare depois da Activity já ter sido destruída
        dashboardHandler.removeCallbacksAndMessages(null)
    }

    // Total de mensagens não vistas
    private fun carregarBadgeAvisos() {
        avisosRepository.carregarSecoes(
            onSuccess = { secoes ->
                val total = secoes.sumOf { it.naoVistasCount }
                if (total > 0) {
                    textWarningBadge.text = total.toString()
                    textWarningBadge.visibility = View.VISIBLE
                } else {
                    textWarningBadge.visibility = View.GONE
                }
            },
            onError = { }
        )
    }

    private fun startComponents() {
        buttonMenu = findViewById(R.id.button_menu)
        buttonWarning = findViewById(R.id.warning)
        textWarningBadge = findViewById(R.id.text_warning_badge)
        buttonFinance = findViewById(R.id.finance)
        buttonSimulation = findViewById(R.id.simulation)
        buttonList = findViewById(R.id.list)
        buttonInsertExcerpt = findViewById(R.id.insertExcerpt)
        buttonTutorial = findViewById(R.id.tutorial)
        tabSimulacoes = findViewById(R.id.tab_dashboard_simulacoes)
        tabMovimentacoes = findViewById(R.id.tab_dashboard_movimentacoes)
        tabCartoes = findViewById(R.id.tab_dashboard_cartoes)
        recyclerDashboard = findViewById(R.id.recycler_dashboard)
        progressDashboard = findViewById(R.id.progress_dashboard)
        drawerLayout = findViewById(R.id.drawer_layout)
        textPanelGreeting = findViewById(R.id.text_panel_greeting)
        buttonPanelClose = findViewById(R.id.button_panel_close)
        buttonPanelName = findViewById(R.id.button_panel_name)
        buttonPanelEmail = findViewById(R.id.button_panel_email)
        buttonPanelPassword = findViewById(R.id.button_panel_password)
        buttonPanelDeleteAccount = findViewById(R.id.button_panel_delete_account)
        buttonPanelPrivacyPolicy = findViewById(R.id.button_panel_privacy_policy)
        buttonPanelExit = findViewById(R.id.button_panel_exit)
        buttonPanelTestMode = findViewById(R.id.button_panel_test_mode)
        linePanelTestMode = findViewById(R.id.line_panel_test_mode)
        buttonPanelExportData = findViewById(R.id.button_panel_export_data)
        cardBoasVindas = findViewById(R.id.card_boas_vindas)
        textBoasVindasTitulo = findViewById(R.id.text_boas_vindas_titulo)
        buttonFecharBoasVindas = findViewById(R.id.button_fechar_boas_vindas)
    }

    private fun verificarRetencaoExtrato() {
        if (retencaoJaVerificadaNesteProcesso) return
        retencaoJaVerificadaNesteProcesso = true
        val umAnoAtras = LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        statementTransactionRepository.loadTransacoesAntigas(
            antesDe = umAnoAtras,
            onSuccess = { transacoes ->
                if (transacoes.isNotEmpty()) {
                    perguntarLimpezaDeExtratoAntigo(transacoes)
                }
            },
            onError = {}
        )
    }

    private fun perguntarLimpezaDeExtratoAntigo(transacoes: List<StatementTransactionRecord>) {
        AlertDialog.Builder(this)
            .setTitle("Extrato antigo encontrado")
            .setMessage(
                "Foram encontrados ${transacoes.size} lançamentos de extrato importados há mais de 1 ano. " +
                    "Manter dados antigos guardados sem necessidade não segue as boas práticas de proteção de dados. " +
                    "Deseja apagar esses lançamentos agora? Essa ação não pode ser desfeita."
            )
            .setPositiveButton("Apagar") { _, _ ->
                statementTransactionRepository.apagarTransacoes(
                    ids = transacoes.map { it.id },
                    onSuccess = { Toast.makeText(this, "Extrato antigo apagado.", Toast.LENGTH_SHORT).show() },
                    onError = { erro -> Toast.makeText(this, "Não foi possível apagar: $erro", Toast.LENGTH_LONG).show() }
                )
            }
            .setNegativeButton("Manter por enquanto", null)
            .show()
    }

    private fun verificarBoasVindas() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection(FirestoreCollections.USERS).document(uid)
            .get()
            .addOnSuccessListener { documento ->
                val jaExibida = documento?.getBoolean("boasVindasExibida") ?: true
                if (jaExibida) return@addOnSuccessListener
                val nome = documento?.getString("nome").orEmpty()
                exibirCardBoasVindas(uid, nome)
            }
            .addOnFailureListener {}
    }

    private fun exibirCardBoasVindas(uid: String, nome: String) {
        textBoasVindasTitulo.text = if (nome.isNotBlank()) {
            getString(R.string.Boas_Vindas_Titulo, nome)
        } else {
            getString(R.string.Boas_Vindas_Titulo_Sem_Nome)
        }
        cardBoasVindas.visibility = View.VISIBLE
        buttonFecharBoasVindas.setOnClickListener {
            cardBoasVindas.visibility = View.GONE
            FirebaseFirestore.getInstance().collection(FirestoreCollections.USERS).document(uid)
                .set(mapOf("boasVindasExibida" to true), SetOptions.merge())
        }
    }

    private companion object {
        const val DASHBOARD_LOADING_MIN_MS = 1000L
        var retencaoJaVerificadaNesteProcesso = false
    }
}
