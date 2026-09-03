package com.luamuniz.dinlux.simulation

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.finance.Bank
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Acesso a dados (Model) das simulações.
 *
 * Guarda dois níveis: a simulação em si na
 * coleção FirestoreCollections.SIMULATIONS, e os lançamentos de compra/economia
 * na coleção FirestoreCollections.SIMULATION_ENTRIES, cada lançamento aponta de
 * volta pro projeto pelo campo simulationId. Coleção plana (em vez de subcoleção),
 * para facilitar consultas futuras do tipo "todos os lançamentos que usam o cartão X,
 * em qualquer simulação" (necessário pro cálculo de limite disponível)
 */
class SimulationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun simulationsRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.SIMULATIONS)

    private fun entriesRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.SIMULATION_ENTRIES)

    fun loadSimulations(onSuccess: (List<Simulation>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        simulationsRef(uid).get()
            .addOnSuccessListener { snapshot ->
                val simulations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Simulation::class.java)?.apply { id = doc.id }
                }
                    // Documentos com título vazio são sobras do modelo antigo de simulação
                    // (antes da reestruturação em projeto + lançamentos). Não devem aparecer
                    // na lista nova. A tela de criação já impede título vazio daqui pra frente
                    .filter { it.title.isNotBlank() }
                    .sortedByDescending { it.createdAt }
                onSuccess(simulations)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar simulações") }
    }

    fun createSimulation(title: String, active: Boolean = false, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        simulationsRef(uid).add(Simulation(title = title, active = active))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao criar simulação") }
    }

    fun renameSimulation(id: String, newTitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        simulationsRef(uid).document(id).update("title", newTitle)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear simulação") }
    }

    /**
     * Desativa uma simulação ativa
     */
    fun setActive(id: String, active: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        simulationsRef(uid).document(id).update("active", active)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar simulação") }
    }

    /**
     * Ativa uma simulação inativa
     */
    fun activateSimulation(
        simulationId: String,
        entries: List<SimulationEntry>,
        banks: List<Bank>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        val zoneId = ZoneId.systemDefault()
        val hoje = LocalDate.now()
        val hojeMillis = hoje.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val batch = db.batch()
        entries.forEach { entry ->
            val ref = entriesRef(uid).document(entry.id)
            if (entry.type == SimulationEntryType.SAVINGS) {
                val inicioAntigo = Instant.ofEpochMilli(entry.startDate).atZone(zoneId).toLocalDate()
                val fimAntigo = Instant.ofEpochMilli(entry.endDate).atZone(zoneId).toLocalDate()
                val meses = SimulationCalculator.calcularMesesEntre(inicioAntigo, fimAntigo)
                val novoFim = hoje.plusMonths((meses - 1).coerceAtLeast(0).toLong())
                val novoValorMensal = SimulationCalculator.calcularValorMensalEconomia(entry.targetValue, hoje, novoFim)
                batch.update(
                    ref,
                    mapOf(
                        "startDate" to hojeMillis,
                        "endDate" to novoFim.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                        "monthlyAmount" to novoValorMensal,
                        "savedAmount" to 0.0,
                        "avisosArquivado" to false,
                        "ultimoPeriodoVisto" to 0L,
                        "confirmedPeriods" to emptyList<Long>()
                    )
                )
            } else {
                val novoFechamento = if (entry.paymentMethod == PaymentMethod.CREDIT) {
                    banks.firstOrNull { it.id == entry.bankId }
                        ?.cards?.firstOrNull { it.id == entry.cardId }
                        ?.closingDay ?: entry.cardClosingDay
                } else {
                    entry.cardClosingDay
                }
                batch.update(
                    ref,
                    mapOf(
                        "createdAt" to hojeMillis,
                        "cardClosingDay" to novoFechamento,
                        "paidInstallments" to 0,
                        "avisosArquivado" to false,
                        "ultimoPeriodoVisto" to 0L,
                        "confirmedPeriods" to emptyList<Long>()
                    )
                )
            }
        }
        batch.update(simulationsRef(uid).document(simulationId), "active", true)

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao ativar simulação") }
    }

    fun deleteSimulation(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        // Exclui também os lançamentos (compras/economias) dessa simulação
        entriesRef(uid).whereEqualTo("simulationId", id).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(simulationsRef(uid).document(id))
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Erro ao excluir simulação") }
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir simulação") }
    }
}
