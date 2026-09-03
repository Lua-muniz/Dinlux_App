package com.luamuniz.dinlux.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SimulationCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun millisDe(data: LocalDate): Long =
        data.atStartOfDay(zoneId).toInstant().toEpochMilli()

    private fun compraCredito(
        totalValue: Double,
        installments: Int,
        installmentValue: Double,
        paidInstallments: Int = 0,
        cardClosingDay: Int = 10,
        createdAt: LocalDate = LocalDate.of(2026, 1, 1),
        confirmedPeriods: List<Long> = emptyList()
    ) = SimulationEntry(
        type = SimulationEntryType.PURCHASE,
        paymentMethod = PaymentMethod.CREDIT,
        totalValue = totalValue,
        installments = installments,
        installmentValue = installmentValue,
        paidInstallments = paidInstallments,
        cardClosingDay = cardClosingDay,
        createdAt = millisDe(createdAt),
        confirmedPeriods = confirmedPeriods
    )

    private fun economia(
        targetValue: Double,
        monthlyAmount: Double,
        inicio: LocalDate,
        fim: LocalDate,
        confirmedPeriods: List<Long> = emptyList()
    ) = SimulationEntry(
        type = SimulationEntryType.SAVINGS,
        targetValue = targetValue,
        monthlyAmount = monthlyAmount,
        startDate = millisDe(inicio),
        endDate = millisDe(fim),
        confirmedPeriods = confirmedPeriods
    )

    @Test
    fun `parcela sem juros divide o valor total igualmente em 12 parcelas`() {
        val parcela = SimulationCalculator.calcularParcela(1200.0, 12, 0.0)
        assertEquals(100.0, parcela, 0.001)
    }

    @Test
    fun `parcela com juros usa tabela Price e cada parcela custa mais que a divisao simples`() {
        val semJuros = SimulationCalculator.calcularParcela(3000.0, 3, 0.0)
        val comJuros = SimulationCalculator.calcularParcela(3000.0, 3, 5.0)
        assertTrue(comJuros > semJuros)
    }

    @Test
    fun `total com juros de 12 parcelas e a parcela vezes 12`() {
        val parcela = SimulationCalculator.calcularParcela(6000.0, 12, 3.5)
        val total = SimulationCalculator.calcularTotalComJuros(parcela, 12)
        assertEquals(parcela * 12, total, 0.001)
        assertTrue(total > 6000.0)
    }

    @Test
    fun `compra em credito antes do fechamento entra na fatura do mesmo mes`() {
        val criadaEm = LocalDate.of(2026, 3, 5)
        val mesInicio = SimulationCalculator.calcularMesInicioCompra(millisDe(criadaEm), PaymentMethod.CREDIT, 10)
        assertEquals(YearMonth.of(2026, 3), mesInicio)
    }

    @Test
    fun `compra em credito depois do fechamento entra na fatura do mes seguinte`() {
        val criadaEm = LocalDate.of(2026, 3, 15)
        val mesInicio = SimulationCalculator.calcularMesInicioCompra(millisDe(criadaEm), PaymentMethod.CREDIT, 10)
        assertEquals(YearMonth.of(2026, 4), mesInicio)
    }

    @Test
    fun `compra em debito comeca sempre no mes da criacao`() {
        val criadaEm = LocalDate.of(2026, 3, 25)
        val mesInicio = SimulationCalculator.calcularMesInicioCompra(millisDe(criadaEm), PaymentMethod.DEBIT, 10)
        assertEquals(YearMonth.of(2026, 3), mesInicio)
    }

    @Test
    fun `mes final de uma compra de 12 parcelas fica 11 meses depois do inicio`() {
        val mesFim = SimulationCalculator.calcularMesFimCompra(YearMonth.of(2026, 1), 12)
        assertEquals(YearMonth.of(2026, 12), mesFim)
    }

    @Test
    fun `mes final de uma compra de 3 parcelas fica 2 meses depois do inicio`() {
        val mesFim = SimulationCalculator.calcularMesFimCompra(YearMonth.of(2026, 6), 3)
        assertEquals(YearMonth.of(2026, 8), mesFim)
    }

    @Test
    fun `parcelas nao pagas de uma compra de 12 parcelas com 5 ja confirmadas`() {
        assertEquals(7, SimulationCalculator.calcularParcelasNaoPagas(12, 5))
    }

    @Test
    fun `parcelas nao pagas nunca fica negativa mesmo com dado inconsistente`() {
        assertEquals(0, SimulationCalculator.calcularParcelasNaoPagas(3, 5))
    }

    @Test
    fun `valor em aberto de uma compra de 12 parcelas com 4 confirmadas`() {
        val valor = SimulationCalculator.calcularValorParcelasNaoPagas(150.0, 12, 4)
        assertEquals(150.0 * 8, valor, 0.001)
    }

    @Test
    fun `limite disponivel desconta so as parcelas ainda nao confirmadas de compras em credito`() {
        val compra12x = compraCredito(totalValue = 1200.0, installments = 12, installmentValue = 100.0, paidInstallments = 3)
        val compra3x = compraCredito(totalValue = 900.0, installments = 3, installmentValue = 300.0, paidInstallments = 0)
        val disponivel = SimulationCalculator.calcularLimiteDisponivel(
            limiteTotal = 2000.0,
            comprasCredito = listOf(compra12x, compra3x)
        )
        assertEquals(2000.0 - (9 * 100.0) - (3 * 300.0), disponivel, 0.001)
    }

    @Test
    fun `limite disponivel tambem desconta o uso manual fora de simulacao`() {
        val disponivel = SimulationCalculator.calcularLimiteDisponivel(
            limiteTotal = 1000.0,
            comprasCredito = emptyList(),
            usedAmount = 250.0
        )
        assertEquals(750.0, disponivel, 0.001)
    }

    @Test
    fun `limite disponivel pode ficar negativo quando as compras passam do limite cadastrado`() {
        val compra = compraCredito(totalValue = 3000.0, installments = 12, installmentValue = 250.0, paidInstallments = 0)
        val disponivel = SimulationCalculator.calcularLimiteDisponivel(limiteTotal = 1000.0, comprasCredito = listOf(compra))
        assertTrue(disponivel < 0.0)
    }

    @Test
    fun `valor da fatura atual soma so as compras ainda nao totalmente pagas`() {
        val comPendencia = compraCredito(totalValue = 1200.0, installments = 12, installmentValue = 100.0, paidInstallments = 6)
        val quitada = compraCredito(totalValue = 300.0, installments = 3, installmentValue = 100.0, paidInstallments = 3)
        val fatura = SimulationCalculator.calcularValorFaturaAtual(listOf(comPendencia, quitada))
        assertEquals(100.0, fatura, 0.001)
    }

    @Test
    fun `ciclos de uma compra de 12 parcelas so mostram os meses ja vencidos`() {
        val compra = compraCredito(
            totalValue = 1200.0,
            installments = 12,
            installmentValue = 100.0,
            cardClosingDay = 10,
            createdAt = LocalDate.of(2026, 1, 5)
        )
        val hoje = LocalDate.of(2026, 4, 20)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 15, hoje = hoje)
        assertEquals(listOf(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3), YearMonth.of(2026, 4)), ciclos.map { it.first })
    }

    @Test
    fun `ciclo do mes atual so aparece depois do dia de vencimento passar`() {
        val compra = compraCredito(
            totalValue = 1200.0,
            installments = 12,
            installmentValue = 100.0,
            cardClosingDay = 10,
            createdAt = LocalDate.of(2026, 1, 5)
        )
        val antesDoVencimento = LocalDate.of(2026, 1, 10)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 15, hoje = antesDoVencimento)
        assertTrue(ciclos.isEmpty())
    }

    @Test
    fun `ciclo confirmado aparece mesmo antes do vencimento chegar`() {
        val mesInicio = LocalDate.of(2026, 1, 5)
        val periodoJaneiro = millisDe(LocalDate.of(2026, 1, 1))
        val compra = compraCredito(
            totalValue = 1200.0,
            installments = 12,
            installmentValue = 100.0,
            cardClosingDay = 10,
            createdAt = mesInicio,
            confirmedPeriods = listOf(periodoJaneiro)
        )
        val antesDoVencimento = LocalDate.of(2026, 1, 8)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 15, hoje = antesDoVencimento)
        assertEquals(1, ciclos.size)
        assertTrue(ciclos.first().second)
    }

    @Test
    fun `compra de 3 parcelas para de gerar ciclos depois da 3a parcela mesmo que hoje avance mais`() {
        val compra = compraCredito(
            totalValue = 900.0,
            installments = 3,
            installmentValue = 300.0,
            cardClosingDay = 10,
            createdAt = LocalDate.of(2026, 1, 5)
        )
        val hoje = LocalDate.of(2026, 12, 1)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 15, hoje = hoje)
        assertEquals(3, ciclos.size)
        assertEquals(YearMonth.of(2026, 3), ciclos.last().first)
    }

    @Test
    fun `dia de vencimento maior que o tamanho do mes de fevereiro usa o ultimo dia do mes`() {
        val compra = compraCredito(
            totalValue = 300.0,
            installments = 1,
            installmentValue = 300.0,
            cardClosingDay = 10,
            createdAt = LocalDate.of(2026, 2, 1)
        )
        val hoje = LocalDate.of(2026, 2, 28)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 31, hoje = hoje)
        assertEquals(1, ciclos.size)
    }

    @Test
    fun `compra sem cartao valido nao gera nenhum ciclo`() {
        val compra = compraCredito(totalValue = 300.0, installments = 3, installmentValue = 100.0)
        val ciclos = SimulationCalculator.listarCiclosCompra(compra, diaVencimento = 0, hoje = LocalDate.now())
        assertTrue(ciclos.isEmpty())
    }

    @Test
    fun `periodos de uma economia de 6 meses aparecem um por mes ja decorrido`() {
        val meta = economia(
            targetValue = 3000.0,
            monthlyAmount = 500.0,
            inicio = LocalDate.of(2026, 1, 1),
            fim = LocalDate.of(2026, 6, 30)
        )
        val hoje = LocalDate.of(2026, 3, 15)
        val periodos = SimulationCalculator.listarPeriodosEconomia(meta, hoje)
        assertEquals(listOf(YearMonth.of(2026, 1), YearMonth.of(2026, 2), YearMonth.of(2026, 3)), periodos.map { it.first })
    }

    @Test
    fun `meses entre inicio e fim de uma economia anual conta 12 meses inclusive`() {
        val meses = SimulationCalculator.calcularMesesEntre(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(12, meses)
    }

    @Test
    fun `valor mensal de uma economia anual e a meta dividida por 12`() {
        val valorMensal = SimulationCalculator.calcularValorMensalEconomia(12000.0, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        assertEquals(1000.0, valorMensal, 0.001)
    }
}
