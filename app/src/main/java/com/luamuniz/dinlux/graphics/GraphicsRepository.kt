package com.luamuniz.dinlux.graphics

import com.luamuniz.dinlux.excerpt.StatementTransactionRepository
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.simulation.Simulation
import com.luamuniz.dinlux.simulation.SimulationCalculator
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationEntryRepository
import com.luamuniz.dinlux.simulation.SimulationEntryType
import com.luamuniz.dinlux.simulation.SimulationRepository
import kotlin.math.abs

/**
 * Monta os dados do dashboard de gráficos embutido na Home — as três abas: Simulações
 * (carregarGraficoSimulacoes), Movimentações e Cartões
 */
class GraphicsRepository(
    private val simulationRepository: SimulationRepository = SimulationRepository(),
    private val entryRepository: SimulationEntryRepository = SimulationEntryRepository(),
    private val bankRepository: BankRepository = BankRepository(),
    private val statementTransactionRepository: StatementTransactionRepository = StatementTransactionRepository()
) {
    fun carregarGraficoSimulacoes(
        onSuccess: (List<DashboardSimulationSection>) -> Unit,
        onError: (String) -> Unit
    ) {
        bankRepository.loadBanks(
            onSuccess = { bancos ->
                val nomesPorBanco = bancos.associateBy({ it.id }, { it.name })
                simulationRepository.loadSimulations(
                    onSuccess = { simulations ->
                        val ativas = simulations.filter { it.active }
                        if (ativas.isEmpty()) {
                            onSuccess(emptyList())
                            return@loadSimulations
                        }
                        carregarSecoesDeCadaAtiva(ativas, nomesPorBanco, onSuccess, onError)
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun carregarSecoesDeCadaAtiva(
        ativas: List<Simulation>,
        nomesPorBanco: Map<String, String>,
        onSuccess: (List<DashboardSimulationSection>) -> Unit,
        onError: (String) -> Unit
    ) {
        val secoes = arrayOfNulls<DashboardSimulationSection>(ativas.size)
        var restantes = ativas.size
        var falhou = false

        ativas.forEachIndexed { index, simulation ->
            entryRepository.loadEntries(
                simulationId = simulation.id,
                onSuccess = { entries ->
                    if (falhou) return@loadEntries
                    secoes[index] = DashboardSimulationSection(
                        simulationTitle = simulation.title,
                        entityCharts = montarGraficosPorEntidade(entries, nomesPorBanco),
                        progressoGeral = if (entries.isEmpty()) 0.0 else entries.map(::proporcaoEntry).average()
                    )
                    restantes--
                    if (restantes == 0) onSuccess(secoes.filterNotNull())
                },
                onError = { message ->
                    if (!falhou) {
                        falhou = true
                        onError(message)
                    }
                }
            )
        }
    }

    // Agrupa os lançamentos de uma simulação por banco+cartão (cartão vazio pra
    // débito/economia), a MESMA chave usada pelos Grupos do canvas e monta um gráfico
    // por grupo, ordenado por banco/cartão
    private fun montarGraficosPorEntidade(
        entries: List<SimulationEntry>,
        nomesPorBanco: Map<String, String>
    ): List<DashboardEntityChart> {
        if (entries.isEmpty()) {
            return listOf(DashboardEntityChart(label = null, colorSourceId = "", entries = emptyList()))
        }

        fun nomeBancoAtual(entry: SimulationEntry) = nomesPorBanco[entry.bankId] ?: "Banco excluído"

        return entries
            .groupBy { chaveEntidade(it) }
            .values
            .sortedWith(compareBy({ nomeBancoAtual(it.first()) }, { it.first().cardLabel }))
            .map { entriesDaEntidade ->
                val primeira = entriesDaEntidade.first()
                val rotulo = if (primeira.cardLabel.isNotBlank()) {
                    "${nomeBancoAtual(primeira)} — ${primeira.cardLabel}"
                } else {
                    "${nomeBancoAtual(primeira)} — Débito"
                }
                DashboardEntityChart(
                    label = rotulo,
                    colorSourceId = idParaCor(primeira),
                    entries = entriesDaEntidade
                        .sortedBy { it.createdAt }
                        .map(::entryParaBarra)
                )
            }
    }

    private fun chaveEntidade(entry: SimulationEntry): String = "${entry.bankId}|${entry.cardId}"
    private fun idParaCor(entry: SimulationEntry): String = entry.cardId.ifEmpty { entry.bankId }

    private fun entryParaBarra(entry: SimulationEntry): DashboardBarChartView.DashboardBarEntry {
        val (valor, max) = if (entry.type == SimulationEntryType.SAVINGS) {
            entry.savedAmount to entry.targetValue
        } else {
            entry.paidInstallments.toDouble() to entry.installments.toDouble()
        }
        return DashboardBarChartView.DashboardBarEntry(
            label = entry.title,
            value = valor,
            max = max,
            color = 0,
            contornoDestacado = entry.type == SimulationEntryType.SAVINGS
        )
    }

    private fun proporcaoEntry(entry: SimulationEntry): Double {
        val (valor, max) = if (entry.type == SimulationEntryType.SAVINGS) {
            entry.savedAmount to entry.targetValue
        } else {
            entry.paidInstallments.toDouble() to entry.installments.toDouble()
        }
        return if (max > 0) (valor / max).coerceIn(0.0, 1.0) else 0.0
    }

    fun carregarGraficoMovimentacoes(
        onSuccess: (List<DashboardBankPieCard>) -> Unit,
        onError: (String) -> Unit
    ) {
        bankRepository.loadBanks(
            onSuccess = { bancos ->
                if (bancos.isEmpty()) {
                    onSuccess(emptyList())
                    return@loadBanks
                }
                carregarTransacoesDeCadaBanco(bancos, onSuccess, onError)
            },
            onError = onError
        )
    }

    private fun carregarTransacoesDeCadaBanco(
        bancos: List<Bank>,
        onSuccess: (List<DashboardBankPieCard>) -> Unit,
        onError: (String) -> Unit
    ) {
        val cards = arrayOfNulls<DashboardBankPieCard>(bancos.size)
        var restantes = bancos.size
        var falhou = false

        bancos.forEachIndexed { index, banco ->
            statementTransactionRepository.loadTransactions(
                bankId = banco.id,
                onSuccess = { transacoes ->
                    if (falhou) return@loadTransactions
                    val entrada = transacoes.filter { it.credit }.sumOf { it.amount }
                    val saida = transacoes.filter { !it.credit }.sumOf { abs(it.amount) }
                    cards[index] = DashboardBankPieCard(
                        bankLabel = banco.name,
                        colorSourceId = banco.id,
                        entrada = entrada,
                        saida = saida
                    )
                    restantes--
                    // Banco mais usado primeiro (maior volume somado de entrada+saída)
                    if (restantes == 0) {
                        onSuccess(cards.filterNotNull().sortedByDescending { it.entrada + it.saida })
                    }
                },
                onError = { message ->
                    if (!falhou) {
                        falhou = true
                        onError(message)
                    }
                }
            )
        }
    }

    fun carregarGraficoCartoes(
        onSuccess: (List<DashboardCardDonut>) -> Unit,
        onError: (String) -> Unit
    ) {
        bankRepository.loadBanks(
            onSuccess = { bancos ->
                val todosOsCartoes = bancos.flatMap { banco -> banco.cards.map { banco to it } }
                if (todosOsCartoes.isEmpty()) {
                    onSuccess(emptyList())
                    return@loadBanks
                }
                entryRepository.loadActiveCreditPurchases { comprasAtivas ->
                    val cards = todosOsCartoes.map { (banco, card) ->
                        val comprasDoCartao = comprasAtivas.filter { it.bankId == banco.id && it.cardId == card.id }
                        val disponivelBruto = SimulationCalculator.calcularLimiteDisponivel(
                            card.limit,
                            comprasDoCartao,
                            card.usedAmount
                        )
                        val disponivel = disponivelBruto.coerceAtLeast(0.0)
                        val usado = (card.limit - disponivelBruto).coerceAtLeast(0.0)
                        DashboardCardDonut(
                            cardLabel = card.label,
                            bankName = banco.name,
                            colorSourceId = card.id,
                            usado = usado,
                            disponivel = disponivel,
                            closingDay = card.closingDay,
                            dueDay = card.dueDay
                        )
                    }
                    onSuccess(cards)
                }
            },
            onError = onError
        )
    }
}
