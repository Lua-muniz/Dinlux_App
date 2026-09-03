package com.luamuniz.dinlux.simulation

/**
 * Um Grupo é um grafo completo de até MAX_NODES_PER_GROUP nós dentro de uma
 * Simulation, todos do mesmo banco (e mesmo cartão, se for crédito). Quando um Grupo
 * enche, um novo é criado automaticamente com o próximo número sequencial global da
 * simulação (Grupo 1, Grupo 2, Grupo 3... na ordem de criação, não por banco/cartão)
 */
data class SimulationGroup(
    var id: String = "",
    var simulationId: String = "",
    var name: String = "",
    var order: Int = 0,
    var bankId: String = "",
    var bankName: String = "",
    var cardId: String = "",
    var cardLabel: String = "",
    var entryType: SimulationEntryType = SimulationEntryType.PURCHASE,
    var paymentMethod: PaymentMethod = PaymentMethod.DEBIT,
    var nodeCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MAX_NODES_PER_GROUP = 6
    }
}
