package com.luamuniz.dinlux.export

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.luamuniz.dinlux.R
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportDataActivity : AppCompatActivity() {

    private lateinit var buttonToBack: ImageButton
    private lateinit var buttonExportarJson: MaterialButton
    private lateinit var buttonExportarPdf: MaterialButton
    private lateinit var progressExport: ProgressBar
    private lateinit var textExportStatus: TextView

    private val exportRepository = ExportRepository()

    private var formatoSelecionado: FormatoExportacao? = null

    private enum class FormatoExportacao {
        JSON, PDF
    }

    private val criarArquivo = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val formato = formatoSelecionado
        if (uri == null || formato == null) {
            alternarCarregando(false)
            return@registerForActivityResult
        }
        gerarEExportar(uri, formato)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_export_data)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        buttonToBack = findViewById(R.id.button_toBack)
        buttonExportarJson = findViewById(R.id.button_exportar_json)
        buttonExportarPdf = findViewById(R.id.button_exportar_pdf)
        progressExport = findViewById(R.id.progress_export)
        textExportStatus = findViewById(R.id.text_export_status)

        buttonToBack.setOnClickListener { finish() }

        buttonExportarJson.setOnClickListener {
            formatoSelecionado = FormatoExportacao.JSON
            alternarCarregando(true)
            criarArquivo.launch(nomeArquivoSugerido("json"))
        }

        buttonExportarPdf.setOnClickListener {
            formatoSelecionado = FormatoExportacao.PDF
            alternarCarregando(true)
            criarArquivo.launch(nomeArquivoSugerido("pdf"))
        }
    }

    private fun nomeArquivoSugerido(extensao: String): String {
        val dataFormatada = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR")).format(Date())
        return "dinlux_meus_dados_$dataFormatada.$extensao"
    }

    private fun gerarEExportar(uri: Uri, formato: FormatoExportacao) {
        exportRepository.carregarTudo(
            onSuccess = { dados ->
                try {
                    contentResolver.openOutputStream(uri)?.use { saida ->
                        when (formato) {
                            FormatoExportacao.JSON -> escreverJson(dados, saida)
                            FormatoExportacao.PDF -> escreverPdf(dados, saida)
                        }
                    }
                    Toast.makeText(this, "Seus dados foram exportados com sucesso.", Toast.LENGTH_LONG).show()
                } catch (erro: Exception) {
                    Toast.makeText(this, "Erro ao salvar o arquivo: ${erro.message}", Toast.LENGTH_LONG).show()
                }
                alternarCarregando(false)
            },
            onError = { erro ->
                Toast.makeText(this, "Erro ao carregar seus dados: $erro", Toast.LENGTH_LONG).show()
                alternarCarregando(false)
            }
        )
    }

    private fun escreverJson(dados: DadosExportados, saida: OutputStream) {
        val json = JsonExportBuilder.construir(dados)
        saida.write(json.toByteArray(Charsets.UTF_8))
    }

    private fun escreverPdf(dados: DadosExportados, saida: OutputStream) {
        val documento = PdfExportBuilder.construir(dados)
        documento.writeTo(saida)
        documento.close()
    }

    private fun alternarCarregando(carregando: Boolean) {
        progressExport.visibility = if (carregando) View.VISIBLE else View.GONE
        textExportStatus.visibility = if (carregando) View.VISIBLE else View.GONE
        buttonExportarJson.isEnabled = !carregando
        buttonExportarPdf.isEnabled = !carregando
    }
}
