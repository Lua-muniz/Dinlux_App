package com.luamuniz.dinlux.finance

data class Bank(
    var id: String = "",
    var name: String = "",
    var debit: Double = 0.0,
    var cards: List<Card> = emptyList(),
    var bankCode: String = ""
)

data class Card(
    var id: String = java.util.UUID.randomUUID().toString(),
    var label: String = "",
    var limit: Double = 0.0,
    var closingDay: Int = 0,
    var dueDay: Int = 0,
    var hasInterest: Boolean = false,
    var interestRate: Double = 0.0, // taxa mensal em %
    var usedAmount: Double = 0.0
)
