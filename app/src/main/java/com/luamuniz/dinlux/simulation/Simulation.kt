package com.luamuniz.dinlux.simulation

/**
 * Uma simulação é o "projeto" criado pelo usuário (ex: "Comprar Móveis", "Viagem para a praia")
 * Ao ativar uma simulação inativa, a data de início de cada lançamento (compra ou
 * economia) é recalculada a partir de hoje, mantendo parcelas/meses/juros iguais
 * já que antes ela podia ter sido só um teste feito em qualquer data. Ao desativar uma
 * simulação ativa, nada nos lançamentos muda; ela só para de ser acompanhada pelos
 * Avisos e volta a não descontar nada de verdade
 */
data class Simulation(
    var id: String = "",
    var title: String = "",
    var active: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)
