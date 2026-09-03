package com.luamuniz.dinlux.simulation

/**
 * Tipo de lançamento dentro de uma simulação
 */
enum class SimulationEntryType {
    PURCHASE,
    SAVINGS
}

/**
 * Forma de pagamento de uma compra (só se aplica quando type = PURCHASE).
 *
 * Regra de negócio:
 * - SAVINGS (economia) só pode ser feita em DEBIT
 * - PURCHASE (compra) parcelada pode ser em DEBIT (0% de juros) ou CREDIT
 *   (usa a taxa de juros do próprio cartão, exceto em 1x à vista, nunca tem juros)
 */
enum class PaymentMethod {
    DEBIT,
    CREDIT
}

data class SimulationEntry(
    var id: String = "",
    var simulationId: String = "",
    var groupId: String = "",
    var type: SimulationEntryType = SimulationEntryType.PURCHASE,
    var title: String = "",

    // Preenchidos apenas quando type = PURCHASE
    var paymentMethod: PaymentMethod? = null,
    var bankId: String = "",
    var bankName: String = "",
    var cardId: String = "",
    var cardLabel: String = "",
    var cardClosingDay: Int = 0,
    var totalValue: Double = 0.0,        // valor total da compra, sem juros
    var installments: Int = 1,
    var interestRate: Double = 0.0,      // taxa aplicada (0 se débito ou 1x à vista)
    var installmentValue: Double = 0.0,  // valor de cada parcela (já com juros embutido, se houver)
    var totalWithInterest: Double = 0.0, // soma de todas as parcelas
    var paidInstallments: Int = 0,

    // Preenchidos apenas quando type = SAVINGS
    var targetValue: Double = 0.0,       // meta a ser alcançada
    var startDate: Long = 0L,            // timestamp (epoch millis) do início do período
    var endDate: Long = 0L,              // timestamp (epoch millis) do fim do período
    var monthlyAmount: Double = 0.0,     // calculado: targetValue / número de meses do período
    var savedAmount: Double = 0.0,       // CACHE: quanto já foi confirmado como guardado de verdade

    // Módulo Avisos, histórico de mensagens (comum a compra e economia)
    var avisosArquivado: Boolean = false,
    var ultimoPeriodoVisto: Long = 0L,
    var confirmedPeriods: List<Long> = emptyList(),
    var createdAt: Long = System.currentTimeMillis()
)
