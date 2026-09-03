package com.luamuniz.dinlux.excerpt

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class CsvParseResult(
    val transactions: List<StatementTransaction>,
    // true quando o parser não conseguiu achar as colunas de data e/ou valor em nenhuma
    // linha do arquivo, sinal de que o layout desse banco ainda não é reconhecido
    val colunasNaoReconhecidas: Boolean
)

/**
 * Parser de extrato em CSV, ao contrário do OFX (formato único, padronizado), CSV varia
 * de banco pra banco: nome das colunas, ordem, separador (vírgula ou ponto e vírgula, no
 * Brasil o `;` é mais comum, já que a vírgula é o separador decimal), às vezes um valor já
 * com sinal, às vezes uma coluna à parte dizendo se foi entrada ou saída.
 *
 * Em vez de manter um layout fixo por banco (exigiria uma tabela por banco, difícil de
 * manter), esse parser tenta reconhecer as colunas pelo nome do cabeçalho. A lista foi pensada a
 * partir dos termos mais comuns usados por bancos/fintechs digitais brasileiros, mas é
 * esperado que precise crescer conforme mais bancos reais forem testados
 */
object CsvParser {

    private val NOMES_DATA = listOf("data", "date", "dt")
    private val NOMES_VALOR = listOf("valor", "amount", "value", "vl")
    private val NOMES_DESCRICAO = listOf(
        "descrição", "descricao", "description", "histórico", "historico", "lançamento",
        "lancamento", "identificador", "title", "memo", "detalhes", "tipo", "type",
        "estabelecimento", "beneficiário", "beneficiario", "favorecido", "operação", "operacao"
    )
    private val NOMES_TIPO_EXATO = listOf("tipo", "type", "natureza", "c/d", "d/c", "cd")

    private val FORMATOS_DATA = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy")

    private data class ColunasCsv(val data: Int, val valor: Int, val descricao: Int, val tipo: Int)

    fun parse(conteudo: String): CsvParseResult {
        val linhas = conteudo.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (linhas.isEmpty()) return CsvParseResult(emptyList(), colunasNaoReconhecidas = true)

        // Procura a primeira linha que pareça um cabeçalho de verdade (tem data E valor
        // reconhecíveis), pula qualquer bloco de resumo/preâmbulo antes dela
        for ((indiceLinha, linha) in linhas.withIndex()) {
            val separador = escolherSeparador(linha)
            val cabecalho = dividirLinha(linha, separador).map { it.trim().lowercase() }
            val colunas = reconhecerColunas(cabecalho) ?: continue

            val transacoes = linhas.drop(indiceLinha + 1).mapNotNull { linhaDado ->
                parseLinha(linhaDado, separador, colunas)
            }
            return CsvParseResult(transacoes, colunasNaoReconhecidas = false)
        }

        return CsvParseResult(emptyList(), colunasNaoReconhecidas = true)
    }

    // Reconhece as 4 colunas nessa ordem de prioridade, cada uma só podendo usar uma coluna
    // ainda não reivindicada por um papel anterior (ver KDoc da classe). Retorna null se não
    // achar pelo menos data e valor. Nesse caso a linha não é um cabeçalho válido
    private fun reconhecerColunas(cabecalho: List<String>): ColunasCsv? {
        val usadas = mutableSetOf<Int>()
        val indiceData = indiceNaoUsado(cabecalho, NOMES_DATA, usadas, exato = false)
        val indiceValor = indiceNaoUsado(cabecalho, NOMES_VALOR, usadas, exato = false)
        if (indiceData < 0 || indiceValor < 0) return null

        val indiceTipo = indiceNaoUsado(cabecalho, NOMES_TIPO_EXATO, usadas, exato = true)
        val indiceDescricao = indiceNaoUsado(cabecalho, NOMES_DESCRICAO, usadas, exato = false)

        return ColunasCsv(data = indiceData, valor = indiceValor, descricao = indiceDescricao, tipo = indiceTipo)
    }

    private fun indiceNaoUsado(
        cabecalho: List<String>,
        termos: List<String>,
        usadas: MutableSet<Int>,
        exato: Boolean
    ): Int {
        for (indice in cabecalho.indices) {
            if (indice in usadas) continue
            val coluna = cabecalho[indice]
            val bate = if (exato) termos.any { coluna == it } else termos.any { coluna.contains(it) }
            if (bate) {
                usadas.add(indice)
                return indice
            }
        }
        return -1
    }

    private fun escolherSeparador(cabecalho: String): Char {
        val pontoEVirgula = cabecalho.count { it == ';' }
        val virgula = cabecalho.count { it == ',' }
        return if (pontoEVirgula >= virgula) ';' else ','
    }

    private fun parseLinha(linha: String, separador: Char, colunas: ColunasCsv): StatementTransaction? {
        val campos = dividirLinha(linha, separador)
        if (colunas.data >= campos.size || colunas.valor >= campos.size) return null

        val data = parseData(campos[colunas.data]) ?: return null
        var valor = parseValor(campos[colunas.valor]) ?: return null

        // Coluna de tipo separada (ex: "C"/"D", "Entrada"/"Saída"): quando reconhecida,
        // decide o sinal de verdade. Pega só a magnitude do valor numérico e aplica o
        // sinal certo, em vez de confiar que o número já veio assinado. Só chega aqui se
        // [reconhecerColunas] achou uma coluna de TIPO de verdade (nome exato, não uma
        // coluna de descrição composta tipo "transaction_type") — ver KDoc da classe
        if (colunas.tipo in campos.indices) {
            val tipo = campos[colunas.tipo].lowercase()
            val ehSaida = tipo.startsWith("d") || tipo.contains("saída") || tipo.contains("saida") ||
                tipo.contains("débito") || tipo.contains("debito")
            val ehEntrada = tipo.startsWith("c") || tipo.contains("entrada") ||
                tipo.contains("crédito") || tipo.contains("credito")
            when {
                ehSaida -> valor = -abs(valor)
                ehEntrada -> valor = abs(valor)
            }
        }

        val descricao = campos.getOrNull(colunas.descricao)?.takeIf { it.isNotBlank() } ?: "Lançamento"

        return StatementTransaction(date = data, description = descricao, amount = valor, credit = valor >= 0)
    }

    // Divisão simples por separador, respeitando campos entre aspas (a descrição de um
    // lançamento pode ter vírgula ou ponto e vírgula dentro, entre aspas)
    private fun dividirLinha(linha: String, separador: Char): List<String> {
        val campos = mutableListOf<String>()
        val atual = StringBuilder()
        var entreAspas = false
        for (c in linha) {
            when {
                c == '"' -> entreAspas = !entreAspas
                c == separador && !entreAspas -> {
                    campos.add(atual.toString())
                    atual.clear()
                }
                else -> atual.append(c)
            }
        }
        campos.add(atual.toString())
        return campos.map { it.trim().removeSurrounding("\"") }
    }

    private fun parseValor(raw: String): Double? {
        val limpo = raw.trim().replace("R$", "").trim()
        if (limpo.isEmpty()) return null

        val normalizado = if (limpo.contains(",")) {
            limpo.replace(".", "").replace(",", ".")
        } else {
            limpo
        }
        return normalizado.toDoubleOrNull()
    }

    private fun parseData(raw: String): Long? {
        val valor = raw.trim()
        for (formato in FORMATOS_DATA) {
            try {
                val sdf = SimpleDateFormat(formato, Locale.US)
                sdf.isLenient = false
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(valor)?.time
            } catch (e: Exception) {
                // tenta o próximo formato
            }
        }
        return null
    }
}
