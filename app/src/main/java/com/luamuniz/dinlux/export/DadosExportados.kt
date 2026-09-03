package com.luamuniz.dinlux.export

import com.luamuniz.dinlux.excerpt.StatementTransactionRecord
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.list.ShoppingList
import com.luamuniz.dinlux.list.ShoppingListItem
import com.luamuniz.dinlux.simulation.Simulation
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationGroup

data class DadosExportados(
    val nome: String,
    val email: String,
    val termsAcceptedAt: Long?,
    val bancos: List<Bank>,
    val simulacoes: List<Simulation>,
    val lancamentos: List<SimulationEntry>,
    val grupos: List<SimulationGroup>,
    val extrato: List<StatementTransactionRecord>,
    val listas: List<ShoppingList>,
    val itensDeLista: List<ShoppingListItem>
)
