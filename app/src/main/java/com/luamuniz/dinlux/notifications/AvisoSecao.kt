package com.luamuniz.dinlux.notifications

import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationEntryType
import java.time.ZoneId

/**
 * Uma "sessão" dentro do chat de um banco, todas as mensagens de um nó
 */
data class AvisoSecao(
    val entry: SimulationEntry,
    val tipo: SimulationEntryType,
    val titulo: String,
    val subtitulo: String,
    val bankId: String,
    val bankName: String,
    val mensagens: List<AvisoMensagem>,
    val finalizada: Boolean,
    val naoVistasCount: Int,
    val cardFechamentoAlterado: Boolean = false,
    val cardFechamentoAtual: Int = 0
) {
    val ordenacao: Long
        get() {
            val referencia = mensagens.firstOrNull { !it.paga } ?: mensagens.lastOrNull()
            return referencia?.periodo
                ?.atDay(1)
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
                ?: entry.createdAt
        }
}
