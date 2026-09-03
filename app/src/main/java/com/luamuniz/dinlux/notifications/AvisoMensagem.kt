package com.luamuniz.dinlux.notifications

import java.time.YearMonth

/**
 * Uma mensagem dentro da sessão de um nó (compra ou economia)
 */
data class AvisoMensagem(
    val periodo: YearMonth,
    val paga: Boolean
)
