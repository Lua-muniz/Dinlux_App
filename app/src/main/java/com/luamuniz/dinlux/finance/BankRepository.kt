package com.luamuniz.dinlux.finance

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections

/**
 * Acesso a dados (Model) dos bancos/cartões do usuário
 *
 * Extraído do que o Finance.kt já fazia direto com Firestore, pra poder ser reaproveitado
 * também pelo canvas de simulação (menu de cartões cadastrados). O Finance.kt continua
 * com sua própria consulta por enquanto
 */
class BankRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun loadBanks(onSuccess: (List<Bank>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS)
            .get()
            .addOnSuccessListener { snapshot ->
                val banks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Bank::class.java)?.apply { id = doc.id }
                }
                onSuccess(banks)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar bancos") }
    }

    fun updateBankCode(bankId: String, bankCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)
            .update("bankCode", bankCode)
            .addOnFailureListener { onError(it.message ?: "Erro ao salvar código do banco") }
        // onSuccess já dispara aqui: a escrita já foi gravada no cache local do Firestore
        // (offline ou não) e vai sincronizar sozinha quando conectar, não precisa esperar
        // confirmação do servidor pra liberar a tela
        onSuccess()
    }

    // Carrega UM banco por id, usado onde só o saldo atual importa (pergunta de saldo
    // antes de importar extrato, saldo projetado no chat de Avisos), sem precisar buscar
    // a lista inteira.
    fun loadBank(bankId: String, onSuccess: (Bank) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)
            .get()
            .addOnSuccessListener { doc ->
                val bank = doc.toObject(Bank::class.java)?.apply { id = doc.id }
                if (bank != null) onSuccess(bank) else onError("Banco não encontrado")
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar banco") }
    }

    fun updateBankDebit(bankId: String, novoValor: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)
            .update("debit", novoValor)
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar saldo") }
        onSuccess()
    }

    // Desconta um valor do saldo (`debit`) de forma atômica via FieldValue.increment
    fun descontarSaldo(bankId: String, valor: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)
            .update("debit", FieldValue.increment(-valor))
            .addOnFailureListener { onError(it.message ?: "Erro ao lançar o valor") }
        onSuccess()
    }

    // Define (sobrescreve) o `usedAmount` (uso manual fora de Simulação, ver Card.kt) de
    // UM cartão específico dentro do array `cards` do banco. Cartão é um campo de lista
    // de mapas no documento do banco, não uma subcoleção própria, então precisa
    // reescrever o array inteiro (mesmo padrão de leitura+escrita já usado em
    // InsertCard.kt e Finance.kt pra editar/excluir cartão). Usado pelo "Lançar" da
    // Lista tanto pra descontar o valor lançado quanto pra aplicar uma
    // correção de limite disponível quando o usuário informa que o valor real é
    // diferente do calculado
    fun atualizarLimiteUsadoCartao(
        bankId: String,
        cardId: String,
        novoUsedAmount: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val bankRef = db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.BANKS).document(bankId)
        bankRef.get()
            .addOnSuccessListener { snapshot ->
                val bank = snapshot.toObject(Bank::class.java)
                if (bank == null) {
                    onError("Banco não encontrado")
                    return@addOnSuccessListener
                }
                val cartoesAtualizados = bank.cards.map { card ->
                    if (card.id == cardId) card.copy(usedAmount = novoUsedAmount) else card
                }
                bankRef.update("cards", cartoesAtualizados)
                    .addOnFailureListener { onError(it.message ?: "Erro ao lançar no cartão") }
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar o cartão") }
    }
}
