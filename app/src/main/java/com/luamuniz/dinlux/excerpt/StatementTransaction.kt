package com.luamuniz.dinlux.excerpt

/**
 * Um lançamento de extrato bancário já normalizado, não importa se veio de OFX ou CSV, todo parser
 * de extrato produz essa mesma forma
 */
data class StatementTransaction(
    val date: Long,       // epoch millis (meia-noite UTC do dia do lançamento)
    val description: String,
    val amount: Double,   // negativo = saiu da conta, positivo = entrou
    val credit: Boolean   // true = recebido, false = gasto (espelha o sinal, mas explícito)
)
