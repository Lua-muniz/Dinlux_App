package com.luamuniz.dinlux.excerpt

/**
 * Versão persistida (Firestore) de um lançamento importado
 *
 * StatementTransaction é o formato interno que os parsers (OFX/CSV)
 * produzem antes de salvar; esse aqui tem os campos extras necessários pra guardar/
 * listar/desduplicar no banco de dados: `id` do documento, `bankId` e quando
 * foi importado.
 */
data class StatementTransactionRecord(
    var id: String = "",
    var bankId: String = "",
    var date: Long = 0L,
    var description: String = "",
    var amount: Double = 0.0,
    var credit: Boolean = false,
    var importedAt: Long = 0L
)
