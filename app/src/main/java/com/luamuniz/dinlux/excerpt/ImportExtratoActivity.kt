package com.luamuniz.dinlux.excerpt

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.luamuniz.dinlux.R
import com.luamuniz.dinlux.finance.BankRepository
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela principal do módulo de leitura de extrato.
 *
 * Sempre aberta com um banco já definido
 * Formatos aceitos: **OFX e CSV**
 *
 * Leitura do OFX: OfxParser extrai o código do banco (`BANKID`) e a lista de
 * lançamentos. O código é comparado com `Bank.bankCode`. Se o banco ainda não tem um
 * código salvo, a primeira importação bem-sucedida "aprende" ele; se já tinha um salvo e
 * ele não bate com o do arquivo, a importação é BARRADA com um aviso
 */
class ImportExtratoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BANK_ID = "bankId"
        const val EXTRA_BANK_NAME = "bankName"
        const val EXTRA_BANK_CODE = "bankCode"
        private val EXTENSOES_ACEITAS = setOf("ofx", "csv")
    }

    private val bankRepository = BankRepository()
    private val statementTransactionRepository = StatementTransactionRepository()

    private lateinit var buttonToBack: ImageButton
    private lateinit var textBank: TextView
    private lateinit var buttonSelecionarArquivo: TextView
    private lateinit var containerResumoExtrato: View
    private lateinit var textResumoExtrato: TextView
    private lateinit var recyclerResumoExtrato: RecyclerView
    private lateinit var cardSalvarLancamentos: View
    private lateinit var buttonSalvarLancamentos: TextView

    private val extratoAdapter = ExtratoAdapter()

    private var bankId: String = ""
    private var bankName: String = ""
    private var bankCode: String = ""

    // Guardado aqui pra o botão "Salvar" ter acesso ao último resumo mostrado, sem precisar
    // reprocessar o arquivo de novo
    private var transacoesParaSalvar: List<StatementTransaction> = emptyList()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    private val abrirSeletorDeArquivo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) tratarArquivoSelecionado(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_import_extrato)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bankId = intent.getStringExtra(EXTRA_BANK_ID) ?: ""
        bankName = intent.getStringExtra(EXTRA_BANK_NAME) ?: ""
        bankCode = intent.getStringExtra(EXTRA_BANK_CODE) ?: ""

        buttonToBack = findViewById(R.id.button_toBack)
        textBank = findViewById(R.id.text_import_bank)
        buttonSelecionarArquivo = findViewById(R.id.button_selecionar_arquivo)
        containerResumoExtrato = findViewById(R.id.container_resumo_extrato)
        textResumoExtrato = findViewById(R.id.text_resumo_extrato)
        recyclerResumoExtrato = findViewById(R.id.recycler_resumo_extrato)
        cardSalvarLancamentos = findViewById(R.id.card_salvar_lancamentos)
        buttonSalvarLancamentos = findViewById(R.id.button_salvar_lancamentos)

        recyclerResumoExtrato.layoutManager = LinearLayoutManager(this)
        recyclerResumoExtrato.adapter = extratoAdapter

        textBank.text = bankName
        buttonToBack.setOnClickListener { finish() }
        buttonSelecionarArquivo.setOnClickListener { perguntarSaldoAtual() }
        buttonSalvarLancamentos.setOnClickListener { salvarLancamentos() }
    }

    private fun perguntarSaldoAtual() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Ex: 180,00"
        }
        AlertDialog.Builder(this)
            .setTitle("Qual o seu saldo atual?")
            .setMessage("Confira no app ou site de \"$bankName\" e digite o saldo de agora, antes de importar o extrato.")
            .setView(input)
            .setPositiveButton("Continuar") { _, _ ->
                val novoSaldo = input.text.toString().replace(",", ".").toDoubleOrNull()
                if (novoSaldo == null) {
                    Toast.makeText(this, "Informe um saldo válido pra continuar.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                bankRepository.updateBankDebit(
                    bankId = bankId,
                    novoValor = novoSaldo,
                    onSuccess = { abrirSeletorDeArquivo.launch(arrayOf("*/*")) },
                    onError = { erro ->
                        Toast.makeText(this, "Não foi possível atualizar o saldo: $erro", Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun tratarArquivoSelecionado(uri: Uri) {
        val nomeArquivo = nomeDoArquivo(uri)
        val extensao = nomeArquivo.substringAfterLast('.', "").lowercase()

        if (extensao !in EXTENSOES_ACEITAS) {
            Toast.makeText(
                this,
                "Formato não suportado. Envie um arquivo .ofx ou .csv.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        when (extensao) {
            "ofx" -> processarOfx(uri, nomeArquivo)
            "csv" -> processarCsv(uri, nomeArquivo)
        }
    }

    private fun processarOfx(uri: Uri, nomeArquivo: String) {
        val conteudo = lerConteudoTexto(uri)
        if (conteudo.isNullOrBlank()) {
            Toast.makeText(this, "Não foi possível ler esse arquivo.", Toast.LENGTH_LONG).show()
            return
        }

        val resultado = OfxParser.parse(conteudo)
        if (resultado.transactions.isEmpty()) {
            Toast.makeText(this, "Nenhum lançamento encontrado nesse arquivo OFX.", Toast.LENGTH_LONG).show()
            return
        }

        conferirBancoOfxEExibirResumo(resultado, nomeArquivo)
    }

    private fun processarCsv(uri: Uri, nomeArquivo: String) {
        val conteudo = lerConteudoTexto(uri)
        if (conteudo.isNullOrBlank()) {
            Toast.makeText(this, "Não foi possível ler esse arquivo.", Toast.LENGTH_LONG).show()
            return
        }

        val resultado = CsvParser.parse(conteudo)
        if (resultado.colunasNaoReconhecidas) {
            Toast.makeText(
                this,
                "Não consegui reconhecer as colunas desse CSV (data/valor). O layout desse banco ainda não é suportado.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (resultado.transactions.isEmpty()) {
            Toast.makeText(this, "Nenhum lançamento encontrado nesse arquivo CSV.", Toast.LENGTH_LONG).show()
            return
        }

        exibirResumo(resultado.transactions, nomeArquivo)
    }

    private fun lerConteudoTexto(uri: Uri): String? {
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return null

        val cabecalho = String(bytes, Charsets.ISO_8859_1).take(400)
        if (Regex("CHARSET:\\s*1252").containsMatchIn(cabecalho)) {
            return String(bytes, Charset.forName("windows-1252"))
        }

        val comoUtf8 = String(bytes, Charsets.UTF_8)
        return if (comoUtf8.contains('�')) String(bytes, Charset.forName("windows-1252")) else comoUtf8
    }

    private fun conferirBancoOfxEExibirResumo(resultado: OfxParseResult, nomeArquivo: String) {
        val codigoArquivo = resultado.bankId

        if (codigoArquivo != null && bankCode.isNotEmpty() && codigoArquivo != bankCode) {
            AlertDialog.Builder(this)
                .setTitle("Esse extrato não parece ser de \"$bankName\"")
                .setMessage(
                    "O código do banco nesse arquivo ($codigoArquivo) é diferente do código já " +
                        "confirmado antes pra \"$bankName\" ($bankCode). Confira se você selecionou " +
                        "o banco certo antes de importar esse extrato."
                )
                .setPositiveButton("Entendi", null)
                .show()
            return
        }

        if (codigoArquivo != null && bankCode.isEmpty()) {
            bankCode = codigoArquivo
            bankRepository.updateBankCode(bankId, codigoArquivo, onSuccess = {}, onError = {})
        }

        exibirResumo(resultado.transactions, nomeArquivo)
    }

    private fun exibirResumo(transacoesBrutas: List<StatementTransaction>, nomeArquivo: String) {
        val transacoes = transacoesBrutas.sortedBy { it.date }
        val dataInicial = dateFormat.format(Date(transacoes.first().date))
        val dataFinal = dateFormat.format(Date(transacoes.last().date))

        textResumoExtrato.text = "Arquivo selecionado: $nomeArquivo\n\n" +
            "${transacoes.size} lançamento(s) encontrado(s), de $dataInicial a $dataFinal."
        extratoAdapter.atualizar(
            transacoes.reversed().map { ExtratoLinha(it.date, it.description, it.amount, it.credit) }
        )
        containerResumoExtrato.visibility = View.VISIBLE

        transacoesParaSalvar = transacoes
        buttonSalvarLancamentos.isEnabled = true
        buttonSalvarLancamentos.text = "Salvar lançamentos"
        cardSalvarLancamentos.visibility = View.VISIBLE
    }

    // Grava no Firestore via StatementTransactionRepository (ver KDoc da classe e da
    // classe Activity). Apaga o extrato inteiro que já estava salvo pra esse banco antes
    // de gravar os lançamentos deste arquivo.
    // "duplicados" agora conta só repetição dentro do próprio arquivo, não mais contra o
    // que já estava salvo (não sobra mais nada salvo pra comparar). Desabilita o botão
    // enquanto salva pra evitar dois toques em sequência disparando duas gravações ao
    // mesmo tempo
    private fun salvarLancamentos() {
        if (transacoesParaSalvar.isEmpty()) return

        buttonSalvarLancamentos.isEnabled = false
        buttonSalvarLancamentos.text = "Salvando..."

        statementTransactionRepository.salvarNovos(
            bankId = bankId,
            transacoes = transacoesParaSalvar,
            onSuccess = { salvos, duplicadosNoArquivo ->
                val mensagem = when {
                    salvos > 0 && duplicadosNoArquivo > 0 ->
                        "Extrato atualizado: $salvos lançamento(s) salvo(s) " +
                            "($duplicadosNoArquivo repetido(s) dentro do próprio arquivo, ignorados)."
                    salvos > 0 ->
                        "Extrato atualizado: $salvos lançamento(s) salvo(s) em Finanças."
                    else ->
                        "Nenhum lançamento válido encontrado nesse arquivo — o extrato anterior desse banco foi apagado mesmo assim."
                }
                Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show()
                finish()
            },
            onError = { erro ->
                Toast.makeText(this, "Não foi possível salvar: $erro", Toast.LENGTH_LONG).show()
                buttonSalvarLancamentos.isEnabled = true
                buttonSalvarLancamentos.text = "Salvar lançamentos"
            }
        )
    }

    private fun nomeDoArquivo(uri: Uri): String {
        var nome = uri.lastPathSegment ?: "arquivo"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (indice >= 0 && cursor.moveToFirst()) {
                nome = cursor.getString(indice) ?: nome
            }
        }
        return nome
    }
}
