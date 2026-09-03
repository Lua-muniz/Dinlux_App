package com.luamuniz.dinlux.simulation

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.pow

object SimulationCalculator {

    /**
     * Calcula o valor fixo de cada parcela.
     * Sem juros: divisão simples
     * Com juros: Tabela Price (PMT = PV * i / (1 - (1+i)^-n))
     */
    fun calcularParcela(valorTotal: Double, parcelas: Int, taxaJurosMensalPercentual: Double): Double {
        if (parcelas <= 0) return valorTotal
        if (taxaJurosMensalPercentual <= 0.0) {
            return valorTotal / parcelas
        }
        val i = taxaJurosMensalPercentual / 100.0
        val fator = i / (1 - (1 + i).pow(-parcelas))
        return valorTotal * fator
    }

    fun calcularTotalComJuros(valorParcela: Double, parcelas: Int): Double {
        return valorParcela * parcelas
    }

    fun calcularMesesEntre(inicio: LocalDate, fim: LocalDate): Int {
        val mesInicio = YearMonth.from(inicio)
        val mesFim = YearMonth.from(fim)
        val meses = ChronoUnit.MONTHS.between(mesInicio, mesFim).toInt() + 1
        return meses.coerceAtLeast(1)
    }

    /**
     * Valor mensal necessário pra alcançar meta entre inicio e fim: meta dividida
     * pelo número de meses do período
     */
    fun calcularValorMensalEconomia(meta: Double, inicio: LocalDate, fim: LocalDate): Double {
        val meses = calcularMesesEntre(inicio, fim)
        return meta / meses
    }

    /**
     * Mês em que a 1ª parcela de uma compra conta, só pra exibição (não é editável
     * é calculado a partir de dados fixos da compra)
     *
     * Débito não tem fatura: a 1ª parcela já conta no mês da criação do lançamento.
     * Crédito: a 1ª parcela sempre entra no próximo fechamento, se a compra foi feita
     * no dia do fechamento ou antes, entra na fatura que fecha nesse mesmo mês; se foi
     * depois, entra na fatura que fecha só no mês seguinte
     */
    fun calcularMesInicioCompra(dataCriacaoMillis: Long, metodo: PaymentMethod, diaFechamentoCartao: Int): YearMonth {
        val dataCriacao = Instant.ofEpochMilli(dataCriacaoMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val mesCriacao = YearMonth.from(dataCriacao)
        if (metodo == PaymentMethod.DEBIT || diaFechamentoCartao <= 0) {
            return mesCriacao
        }
        return if (dataCriacao.dayOfMonth <= diaFechamentoCartao) mesCriacao else mesCriacao.plusMonths(1)
    }

    /**
     * Mês da última parcela, contando parcelas meses a partir de mesInicio
     */
    fun calcularMesFimCompra(mesInicio: YearMonth, parcelas: Int): YearMonth {
        return mesInicio.plusMonths((parcelas - 1).coerceAtLeast(0).toLong())
    }

    // ---------- MÓDULO AVISOS (confirmações de economia/fatura) ----------

    /**
     * Quantas parcelas de uma compra em crédito ainda não foram confirmadas como pagas
     */
    fun calcularParcelasNaoPagas(parcelasTotais: Int, parcelasPagas: Int): Int {
        return (parcelasTotais - parcelasPagas).coerceAtLeast(0)
    }

    /**
     * Valor ainda em aberto de uma compra em crédito: parcelas não pagas × valor da parcela
     */
    fun calcularValorParcelasNaoPagas(valorParcela: Double, parcelasTotais: Int, parcelasPagas: Int): Double {
        return valorParcela * calcularParcelasNaoPagas(parcelasTotais, parcelasPagas)
    }

    /**
     * Limite disponível de um cartão
     */
    fun calcularLimiteDisponivel(
        limiteTotal: Double,
        comprasCredito: List<SimulationEntry>,
        usedAmount: Double = 0.0
    ): Double {
        val usado = comprasCredito.sumOf {
            calcularValorParcelasNaoPagas(it.installmentValue, it.installments, it.paidInstallments)
        }
        return limiteTotal - usado - usedAmount
    }

    /**
     * Valor da fatura atual de um cartão
     */
    fun calcularValorFaturaAtual(comprasCredito: List<SimulationEntry>): Double {
        return comprasCredito
            .filter { it.paidInstallments < it.installments }
            .sumOf { it.installmentValue }
    }

    /**
     * Histórico completo dos períodos de uma economia, do mês de início até hoje
     */
    fun listarPeriodosEconomia(entry: SimulationEntry, hoje: LocalDate = LocalDate.now()): List<Pair<YearMonth, Boolean>> {
        val zoneId = ZoneId.systemDefault()
        val mesInicio = YearMonth.from(Instant.ofEpochMilli(entry.startDate).atZone(zoneId).toLocalDate())
        val mesFimPeriodo = YearMonth.from(Instant.ofEpochMilli(entry.endDate).atZone(zoneId).toLocalDate())
        val mesAtual = YearMonth.from(hoje)
        val confirmados = entry.confirmedPeriods.toSet()

        val periodos = mutableListOf<Pair<YearMonth, Boolean>>()
        var mes = mesInicio
        while (!mes.isAfter(mesFimPeriodo)) {
            val millis = mes.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val pago = millis in confirmados
            if (pago || !mes.isAfter(mesAtual)) {
                periodos.add(mes to pago)
            } else {
                break // meses futuros (o período ainda nem começou) — lista sempre cronológica
            }
            mes = mes.plusMonths(1)
        }
        return periodos
    }

    /**
     * Histórico completo dos ciclos (parcelas) de uma compra em crédito, do 1º ciclo até
     * o ciclo com vencimento mais recente já vencido
     */
    fun listarCiclosCompra(entry: SimulationEntry, diaVencimento: Int, hoje: LocalDate = LocalDate.now()): List<Pair<YearMonth, Boolean>> {
        if (diaVencimento <= 0 || entry.installments <= 0) return emptyList()
        val zoneId = ZoneId.systemDefault()
        val mesInicio = calcularMesInicioCompra(entry.createdAt, entry.paymentMethod ?: PaymentMethod.CREDIT, entry.cardClosingDay)
        val confirmados = entry.confirmedPeriods.toSet()

        val ciclos = mutableListOf<Pair<YearMonth, Boolean>>()
        for (indice in 0 until entry.installments) {
            val mes = mesInicio.plusMonths(indice.toLong())
            val dia = diaVencimento.coerceIn(1, mes.lengthOfMonth())
            val vencimento = mes.atDay(dia)
            val millis = mes.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val paga = millis in confirmados
            if (paga || !vencimento.isAfter(hoje)) {
                ciclos.add(mes to paga)
            } else {
                break
            }
        }
        return ciclos
    }
}
