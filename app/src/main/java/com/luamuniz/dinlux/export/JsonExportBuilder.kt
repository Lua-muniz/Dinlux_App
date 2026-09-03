package com.luamuniz.dinlux.export

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonExportBuilder {

    fun construir(dados: DadosExportados): String {
        val raiz = JSONObject()
        raiz.put("geradoEm", formatarData(System.currentTimeMillis()))
        raiz.put("nome", dados.nome)
        raiz.put("email", dados.email)
        dados.termsAcceptedAt?.let { raiz.put("termoDePrivacidadeAceitoEm", formatarData(it)) }

        val bancosArray = JSONArray()
        dados.bancos.forEach { banco ->
            val bancoJson = JSONObject()
            bancoJson.put("id", banco.id)
            bancoJson.put("nome", banco.name)
            bancoJson.put("saldo", banco.debit)
            val cartoesArray = JSONArray()
            banco.cards.forEach { cartao ->
                val cartaoJson = JSONObject()
                cartaoJson.put("id", cartao.id)
                cartaoJson.put("bandeira", cartao.label)
                cartaoJson.put("limite", cartao.limit)
                cartaoJson.put("diaFechamento", cartao.closingDay)
                cartaoJson.put("diaVencimento", cartao.dueDay)
                cartaoJson.put("temJuros", cartao.hasInterest)
                cartaoJson.put("taxaJurosMensal", cartao.interestRate)
                cartaoJson.put("usoManualForaDeSimulacao", cartao.usedAmount)
                cartoesArray.put(cartaoJson)
            }
            bancoJson.put("cartoes", cartoesArray)
            bancosArray.put(bancoJson)
        }
        raiz.put("bancos", bancosArray)

        val simulacoesArray = JSONArray()
        dados.simulacoes.forEach { simulacao ->
            val simulacaoJson = JSONObject()
            simulacaoJson.put("id", simulacao.id)
            simulacaoJson.put("titulo", simulacao.title)
            simulacaoJson.put("ativa", simulacao.active)
            simulacaoJson.put("criadaEm", formatarData(simulacao.createdAt))
            simulacoesArray.put(simulacaoJson)
        }
        raiz.put("simulacoes", simulacoesArray)

        val lancamentosArray = JSONArray()
        dados.lancamentos.forEach { lancamento ->
            val lancamentoJson = JSONObject()
            lancamentoJson.put("id", lancamento.id)
            lancamentoJson.put("simulacaoId", lancamento.simulationId)
            lancamentoJson.put("grupoId", lancamento.groupId)
            lancamentoJson.put("tipo", lancamento.type.name)
            lancamentoJson.put("titulo", lancamento.title)
            lancamentoJson.put("formaDePagamento", lancamento.paymentMethod?.name)
            lancamentoJson.put("bancoId", lancamento.bankId)
            lancamentoJson.put("bancoNome", lancamento.bankName)
            lancamentoJson.put("cartaoId", lancamento.cardId)
            lancamentoJson.put("cartaoRotulo", lancamento.cardLabel)
            lancamentoJson.put("cartaoDiaFechamentoNaCompra", lancamento.cardClosingDay)
            lancamentoJson.put("valorTotal", lancamento.totalValue)
            lancamentoJson.put("parcelas", lancamento.installments)
            lancamentoJson.put("taxaJuros", lancamento.interestRate)
            lancamentoJson.put("valorParcela", lancamento.installmentValue)
            lancamentoJson.put("valorTotalComJuros", lancamento.totalWithInterest)
            lancamentoJson.put("parcelasConfirmadas", lancamento.paidInstallments)
            lancamentoJson.put("metaEconomia", lancamento.targetValue)
            lancamentoJson.put("inicioPeriodo", if (lancamento.startDate > 0) formatarData(lancamento.startDate) else null)
            lancamentoJson.put("fimPeriodo", if (lancamento.endDate > 0) formatarData(lancamento.endDate) else null)
            lancamentoJson.put("valorMensalEconomia", lancamento.monthlyAmount)
            lancamentoJson.put("valorGuardado", lancamento.savedAmount)
            lancamentoJson.put("criadoEm", formatarData(lancamento.createdAt))
            lancamentosArray.put(lancamentoJson)
        }
        raiz.put("lancamentos", lancamentosArray)

        val gruposArray = JSONArray()
        dados.grupos.forEach { grupo ->
            val grupoJson = JSONObject()
            grupoJson.put("id", grupo.id)
            grupoJson.put("simulacaoId", grupo.simulationId)
            grupoJson.put("nome", grupo.name)
            grupoJson.put("ordem", grupo.order)
            grupoJson.put("bancoNome", grupo.bankName)
            grupoJson.put("cartaoRotulo", grupo.cardLabel)
            grupoJson.put("tipo", grupo.entryType.name)
            grupoJson.put("formaDePagamento", grupo.paymentMethod.name)
            grupoJson.put("quantidadeDeNos", grupo.nodeCount)
            gruposArray.put(grupoJson)
        }
        raiz.put("grupos", gruposArray)

        val extratoArray = JSONArray()
        dados.extrato.forEach { transacao ->
            val transacaoJson = JSONObject()
            transacaoJson.put("id", transacao.id)
            transacaoJson.put("bancoId", transacao.bankId)
            transacaoJson.put("data", formatarDataCurta(transacao.date))
            transacaoJson.put("descricao", transacao.description)
            transacaoJson.put("valor", transacao.amount)
            transacaoJson.put("recebido", transacao.credit)
            transacaoJson.put("importadoEm", formatarData(transacao.importedAt))
            extratoArray.put(transacaoJson)
        }
        raiz.put("extrato", extratoArray)

        val listasArray = JSONArray()
        dados.listas.forEach { lista ->
            val listaJson = JSONObject()
            listaJson.put("id", lista.id)
            listaJson.put("titulo", lista.title)
            listaJson.put("criadaEm", formatarData(lista.createdAt))
            val itensArray = JSONArray()
            dados.itensDeLista.filter { it.listId == lista.id }.forEach { item ->
                val itemJson = JSONObject()
                itemJson.put("id", item.id)
                itemJson.put("nome", item.name)
                itemJson.put("quantidade", item.quantity)
                itemJson.put("preco", item.price)
                itemJson.put("marcado", item.done)
                itensArray.put(itemJson)
            }
            listaJson.put("itens", itensArray)
            listasArray.put(listaJson)
        }
        raiz.put("listas", listasArray)

        return raiz.toString(2)
    }

    private fun formatarData(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(millis))

    private fun formatarDataCurta(millis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(millis))
}
