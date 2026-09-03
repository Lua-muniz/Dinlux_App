package com.luamuniz.dinlux.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.luamuniz.dinlux.simulation.PaymentMethod
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationEntryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportBuilder {

    private const val LARGURA_PAGINA = 595
    private const val ALTURA_PAGINA = 842
    private const val MARGEM = 40f

    fun construir(dados: DadosExportados): PdfDocument {
        val documento = PdfDocument()
        val escritor = EscritorDeRelatorio(documento)

        escritor.titulo("Dinlux, Meus Dados")
        escritor.linha("Relatório gerado em ${formatarData(System.currentTimeMillis())}")
        escritor.linha("Nome: ${dados.nome}")
        escritor.linha("Email: ${dados.email}")
        dados.termsAcceptedAt?.let {
            escritor.linha("Termo de Privacidade aceito em ${formatarData(it)}")
        }
        escritor.espaco()

        escritor.secao("Bancos e Cartões")
        if (dados.bancos.isEmpty()) {
            escritor.linha("Nenhum banco cadastrado.")
        }
        dados.bancos.forEach { banco ->
            escritor.subtitulo(banco.name)
            escritor.linha("Saldo atual: ${formatarValor(banco.debit)}")
            if (banco.cards.isEmpty()) {
                escritor.linha("Nenhum cartão cadastrado neste banco.")
            }
            banco.cards.forEach { cartao ->
                escritor.linha(
                    "Cartão ${cartao.label}, limite ${formatarValor(cartao.limit)}, " +
                        "fechamento dia ${cartao.closingDay}, vencimento dia ${cartao.dueDay}, " +
                        "juros ${if (cartao.hasInterest) "${cartao.interestRate}% ao mês" else "sem juros"}, " +
                        "uso manual fora de simulação ${formatarValor(cartao.usedAmount)}"
                )
            }
            escritor.espaco()
        }

        escritor.secao("Simulações")
        if (dados.simulacoes.isEmpty()) {
            escritor.linha("Nenhuma simulação cadastrada.")
        }
        dados.simulacoes.forEach { simulacao ->
            escritor.linha(
                "${simulacao.title}, ${if (simulacao.active) "ativa" else "inativa"}, " +
                    "criada em ${formatarData(simulacao.createdAt)}"
            )
        }
        escritor.espaco()

        escritor.secao("Lançamentos de Compra e Economia")
        if (dados.lancamentos.isEmpty()) {
            escritor.linha("Nenhum lançamento cadastrado.")
        }
        dados.lancamentos.forEach { lancamento ->
            escritor.linha(descreverLancamento(lancamento))
        }
        escritor.espaco()

        escritor.secao("Extrato Importado")
        if (dados.extrato.isEmpty()) {
            escritor.linha("Nenhum extrato importado.")
        }
        dados.extrato.sortedByDescending { it.date }.forEach { transacao ->
            escritor.linha(
                "${formatarDataCurta(transacao.date)}, ${transacao.description}, " +
                    "${if (transacao.credit) "recebido" else "gasto"} ${formatarValor(Math.abs(transacao.amount))}"
            )
        }
        escritor.espaco()

        escritor.secao("Listas")
        if (dados.listas.isEmpty()) {
            escritor.linha("Nenhuma lista cadastrada.")
        }
        dados.listas.forEach { lista ->
            escritor.subtitulo(lista.title)
            val itens = dados.itensDeLista.filter { it.listId == lista.id }
            if (itens.isEmpty()) {
                escritor.linha("Nenhum item nesta lista.")
            }
            itens.forEach { item ->
                val detalhes = mutableListOf<String>()
                item.quantity?.let { detalhes.add("quantidade $it") }
                item.price?.let { detalhes.add("preço ${formatarValor(it)}") }
                detalhes.add(if (item.done) "marcado" else "não marcado")
                escritor.linha("${item.name}, ${detalhes.joinToString(", ")}")
            }
            escritor.espaco()
        }

        escritor.finalizar()
        return documento
    }

    private fun descreverLancamento(lancamento: SimulationEntry): String {
        return if (lancamento.type == SimulationEntryType.SAVINGS) {
            "${lancamento.title}, economia, meta ${formatarValor(lancamento.targetValue)}, " +
                "guardado até agora ${formatarValor(lancamento.savedAmount)}, banco ${lancamento.bankName}"
        } else {
            val forma = if (lancamento.paymentMethod == PaymentMethod.CREDIT) "crédito" else "débito"
            val descricaoCartao = if (lancamento.paymentMethod == PaymentMethod.CREDIT) ", cartão ${lancamento.cardLabel}" else ""
            "${lancamento.title}, compra em $forma, valor total ${formatarValor(lancamento.totalValue)}, " +
                "${lancamento.installments}x de ${formatarValor(lancamento.installmentValue)}, " +
                "${lancamento.paidInstallments} parcelas confirmadas, banco ${lancamento.bankName}$descricaoCartao"
        }
    }

    private fun formatarValor(valor: Double): String =
        "R$ " + String.format(Locale("pt", "BR"), "%.2f", valor)

    private fun formatarData(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(millis))

    private fun formatarDataCurta(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(millis))

    private class EscritorDeRelatorio(private val documento: PdfDocument) {
        private val paintTitulo = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val paintSecao = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val paintSubtitulo = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val paintTexto = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.DEFAULT
        }

        private var pagina: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGEM
        private var numeroPagina = 1
        private val larguraUtil = LARGURA_PAGINA - (MARGEM * 2)

        init {
            novaPagina()
        }

        private fun novaPagina() {
            pagina?.let { documento.finishPage(it) }
            val info = PdfDocument.PageInfo.Builder(LARGURA_PAGINA, ALTURA_PAGINA, numeroPagina).create()
            val paginaNova = documento.startPage(info)
            pagina = paginaNova
            canvas = paginaNova.canvas
            y = MARGEM
            numeroPagina++
        }

        private fun garantirEspaco(alturaNecessaria: Float) {
            if (y + alturaNecessaria > ALTURA_PAGINA - MARGEM) {
                novaPagina()
            }
        }

        fun titulo(texto: String) {
            garantirEspaco(28f)
            canvas?.drawText(texto, MARGEM, y + 18f, paintTitulo)
            y += 28f
        }

        fun secao(texto: String) {
            garantirEspaco(24f)
            canvas?.drawText(texto, MARGEM, y + 14f, paintSecao)
            y += 22f
        }

        fun subtitulo(texto: String) {
            garantirEspaco(18f)
            canvas?.drawText(texto, MARGEM, y + 12f, paintSubtitulo)
            y += 18f
        }

        fun linha(texto: String) {
            quebrarLinha(texto, paintTexto).forEach { linhaUnica ->
                garantirEspaco(15f)
                canvas?.drawText(linhaUnica, MARGEM, y + 10f, paintTexto)
                y += 15f
            }
        }

        fun espaco() {
            y += 10f
        }

        fun finalizar() {
            pagina?.let { documento.finishPage(it) }
        }

        private fun quebrarLinha(texto: String, paint: Paint): List<String> {
            val palavras = texto.split(" ")
            val linhas = mutableListOf<String>()
            var linhaAtual = StringBuilder()
            palavras.forEach { palavra ->
                val tentativa = if (linhaAtual.isEmpty()) palavra else "$linhaAtual $palavra"
                if (paint.measureText(tentativa) > larguraUtil && linhaAtual.isNotEmpty()) {
                    linhas.add(linhaAtual.toString())
                    linhaAtual = StringBuilder(palavra)
                } else {
                    linhaAtual = StringBuilder(tentativa)
                }
            }
            if (linhaAtual.isNotEmpty()) {
                linhas.add(linhaAtual.toString())
            }
            return linhas
        }
    }
}
