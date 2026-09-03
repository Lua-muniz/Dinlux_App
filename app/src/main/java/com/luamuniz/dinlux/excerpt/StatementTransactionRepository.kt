package com.luamuniz.dinlux.excerpt

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections

/**
 * Acesso a dados dos lançamentos de extrato já salvos. Coleção plana (`users/{uid}/statementTransactions`), filtrada por `bankId`
 */
class StatementTransactionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun ref(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.STATEMENT_TRANSACTIONS)

    fun loadTransactions(
        bankId: String,
        onSuccess: (List<StatementTransactionRecord>) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        ref(uid)
            .whereEqualTo("bankId", bankId)
            .get()
            .addOnSuccessListener { snapshot ->
                val transacoes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StatementTransactionRecord::class.java)?.apply { id = doc.id }
                }
                onSuccess(transacoes)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar extrato") }
    }

    fun salvarNovos(
        bankId: String,
        transacoes: List<StatementTransaction>,
        onSuccess: (salvos: Int, duplicadosNoArquivo: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        apagarExtratoDoBanco(bankId, onSuccess = {
            // Dedup só entre as linhas DESSE arquivo (ex: o próprio extrato repetindo uma
            // linha) — não tem mais nada salvo pra comparar, já que acabou de apagar tudo.
            val vistas = mutableSetOf<String>()
            val unicas = transacoes.filter { vistas.add(chave(it.date, it.description, it.amount)) }

            if (unicas.isEmpty()) {
                onSuccess(0, transacoes.size)
                return@apagarExtratoDoBanco
            }

            val agora = System.currentTimeMillis()
            val batch = db.batch()
            unicas.forEach { transacao ->
                val doc = ref(uid).document()
                val record = StatementTransactionRecord(
                    id = doc.id,
                    bankId = bankId,
                    date = transacao.date,
                    description = transacao.description,
                    amount = transacao.amount,
                    credit = transacao.credit,
                    importedAt = agora
                )
                batch.set(doc, record)
            }
            batch.commit()
                .addOnSuccessListener { onSuccess(unicas.size, transacoes.size - unicas.size) }
                .addOnFailureListener { onError(it.message ?: "Erro ao salvar lançamentos") }
        }, onError = onError)
    }

    // Apaga TODOS os StatementTransactionRecord já salvos pra esse banco. Usado antes de
    // gravar uma importação nova (ver KDoc de [salvarNovos]) e também
    // por `Finance.excluirBancoAtual()`. Quando o banco é excluído, o extrato importado
    // dele é "algo relacionado" e vai junto
    fun apagarExtratoDoBanco(
        bankId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        ref(uid)
            .whereEqualTo("bankId", bankId)
            .get()
            .addOnSuccessListener { snapshot ->
                apagarEmLotes(snapshot.documents.map { it.reference }, onSuccess, onError)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao apagar extrato anterior") }
    }

    // Firestore limita 500 operações por batch — corta em lotes de 400 (com folga) e
    // confirma um de cada vez, em sequência, só chamando onSuccess quando o último lote
    // terminar. Um extrato normal tem no máximo algumas centenas de lançamentos, então
    // isso raramente precisa de mais de um lote. É só uma proteção pra não
    // falhar silenciosamente se algum dia acontecer.
    private fun apagarEmLotes(
        referencias: List<DocumentReference>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (referencias.isEmpty()) {
            onSuccess()
            return
        }
        val lote = referencias.take(400)
        val resto = referencias.drop(400)
        val batch = db.batch()
        lote.forEach { batch.delete(it) }
        batch.commit()
            .addOnSuccessListener { apagarEmLotes(resto, onSuccess, onError) }
            .addOnFailureListener { onError(it.message ?: "Erro ao apagar extrato anterior") }
    }

    fun loadTransacoesAntigas(
        antesDe: Long,
        onSuccess: (List<StatementTransactionRecord>) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        ref(uid)
            .whereLessThan("date", antesDe)
            .get()
            .addOnSuccessListener { snapshot ->
                val transacoes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(StatementTransactionRecord::class.java)?.apply { id = doc.id }
                }
                onSuccess(transacoes)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar extrato antigo") }
    }

    fun apagarTransacoes(ids: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        apagarEmLotes(ids.map { ref(uid).document(it) }, onSuccess, onError)
    }

    private fun chave(date: Long, description: String, amount: Double): String {
        val valorEmCentavos = Math.round(amount * 100)
        return "$date|${description.trim().lowercase()}|$valorEmCentavos"
    }
}
