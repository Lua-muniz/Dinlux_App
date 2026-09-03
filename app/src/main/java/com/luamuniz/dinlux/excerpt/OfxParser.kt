package com.luamuniz.dinlux.excerpt

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class OfxParseResult(
    val bankId: String?,
    val transactions: List<StatementTransaction>
)

/**
 * Parser de OFX (Open Financial Exchange)
 * (1) cada bloco `<STMTTRN>...</STMTTRN>` sempre fecha, então acha cada um por
 * regex;
 * (2) dentro de cada bloco (e no arquivo inteiro, pro BANKID), lê pares tag/valor
 * simples `<TAG>valor` até a próxima tag ou quebra de linha.
 *
 * Assume o padrão OFX pro sinal do valor (`TRNAMT`): negativo = saiu da conta, positivo =
 * entrou. Descrição usa `MEMO` (mais descritivo na maioria dos bancos BR) com fallback pra
 * `NAME` e por último `TRNTYPE`, pra nunca ficar em branco
 */
object OfxParser {

    private val TAG_VALUE = Regex("""<([A-Za-z0-9.]+)>\s*([^<\r\n]*)""")
    private val STMTTRN_BLOCK = Regex("""<STMTTRN>(.*?)</STMTTRN>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(conteudo: String): OfxParseResult {
        val bankId = TAG_VALUE.findAll(conteudo)
            .firstOrNull { it.groupValues[1].uppercase() == "BANKID" }
            ?.groupValues?.get(2)?.trim()?.takeIf { it.isNotEmpty() }

        val transacoes = STMTTRN_BLOCK.findAll(conteudo)
            .mapNotNull { parseTransacao(it.groupValues[1]) }
            .toList()

        return OfxParseResult(bankId, transacoes)
    }

    private fun parseTransacao(bloco: String): StatementTransaction? {
        val campos = TAG_VALUE.findAll(bloco).associate { it.groupValues[1].uppercase() to it.groupValues[2].trim() }

        val dataStr = campos["DTPOSTED"] ?: return null
        val valorStr = campos["TRNAMT"] ?: return null
        val valor = valorStr.replace(",", ".").toDoubleOrNull() ?: return null
        val data = parseData(dataStr) ?: return null
        val descricao = campos["MEMO"]?.takeIf { it.isNotBlank() }
            ?: campos["NAME"]?.takeIf { it.isNotBlank() }
            ?: campos["TRNTYPE"]?.takeIf { it.isNotBlank() }
            ?: "Lançamento"

        return StatementTransaction(date = data, description = descricao, amount = valor, credit = valor >= 0)
    }

    private fun parseData(raw: String): Long? {
        val digits = raw.takeWhile { it.isDigit() }
        if (digits.length < 8) return null
        val soData = digits.substring(0, 8)
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(soData)?.time
        } catch (e: Exception) {
            null
        }
    }
}
