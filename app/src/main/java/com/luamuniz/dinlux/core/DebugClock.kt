package com.luamuniz.dinlux.core

import java.time.LocalDate

object DebugClock {
    private var dataSimulada: LocalDate? = null

    fun hoje(): LocalDate = dataSimulada ?: LocalDate.now()

    fun dataAtual(): LocalDate? = dataSimulada

    fun definir(data: LocalDate) {
        dataSimulada = data
    }

    fun limpar() {
        dataSimulada = null
    }
}
