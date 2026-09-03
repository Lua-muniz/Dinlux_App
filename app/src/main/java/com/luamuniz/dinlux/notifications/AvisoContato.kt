package com.luamuniz.dinlux.notifications

data class AvisoContato(
    val bankId: String,
    val bankName: String,
    val naoVistasCount: Int,
    val ordenacao: Long
)
