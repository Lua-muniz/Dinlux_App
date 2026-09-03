package com.luamuniz.dinlux.core

/**
 * Nomes centralizados das coleções do Firestore
 *
 * Evita strings soltas pelas Activities e Repositories,
 * qualquer mudança de nome de coleção passa a ser feita em um único lugar
 */
object FirestoreCollections {
    const val USERS = "users"
    const val BANKS = "banks"
    const val SIMULATIONS = "simulations"
    const val SIMULATION_ENTRIES = "simulationEntries"
    const val SIMULATION_GROUPS = "simulationGroups"
    const val STATEMENT_TRANSACTIONS = "statementTransactions"
    const val LISTS = "lists"
    const val LIST_ITEMS = "listItems"
}
