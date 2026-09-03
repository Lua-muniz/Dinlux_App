package com.luamuniz.dinlux.simulation

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Acesso a dados dos lançamentos (compra/economia) e dos Grupos que os organizam
 * dentro de uma simulação
 *
 * Ao criar um lançamento, este repositório decide sozinho em qual Grupo ele entra:
 * procura um Grupo existente com a mesma chave (banco/cartão/tipo) que ainda tenha
 * vaga (< SimulationGroup.MAX_NODES_PER_GROUP nós); se não achar, cria um Grupo novo
 * com o próximo número sequencial da simulação. O `groupId` passado em SimulationEntry é ignorado
 */
class SimulationEntryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val simulationRepository: SimulationRepository = SimulationRepository()
) {

    private fun groupsRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.SIMULATION_GROUPS)

    private fun entriesRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.SIMULATION_ENTRIES)

    fun loadGroups(simulationId: String, onSuccess: (List<SimulationGroup>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        groupsRef(uid)
            .whereEqualTo("simulationId", simulationId)
            .get()
            .addOnSuccessListener { snapshot ->
                val grupos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SimulationGroup::class.java)?.apply { id = doc.id }
                }.sortedBy { it.order }
                onSuccess(grupos)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar Grupos") }
    }

    fun loadEntries(simulationId: String, onSuccess: (List<SimulationEntry>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        entriesRef(uid)
            .whereEqualTo("simulationId", simulationId)
            .get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SimulationEntry::class.java)?.apply { id = doc.id }
                }.sortedBy { it.createdAt }
                onSuccess(entries)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar lançamentos") }
    }

    fun createEntry(entry: SimulationEntry, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        groupsRef(uid)
            .whereEqualTo("simulationId", entry.simulationId)
            .whereEqualTo("bankId", entry.bankId)
            .whereEqualTo("cardId", entry.cardId)
            .whereEqualTo("entryType", entry.type.name)
            .get()
            .addOnSuccessListener { snapshot ->
                val gruposExistentes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SimulationGroup::class.java)?.apply { id = doc.id }
                }
                val grupoComVaga = gruposExistentes
                    .filter { it.nodeCount < SimulationGroup.MAX_NODES_PER_GROUP }
                    .minByOrNull { it.order }

                if (grupoComVaga != null) {
                    adicionarNoGrupo(uid, grupoComVaga, entry, onSuccess, onError)
                } else {
                    criarGrupoNovoENumerar(uid, entry, onSuccess, onError)
                }
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao verificar Grupos") }
    }

    private fun criarGrupoNovoENumerar(
        uid: String,
        entry: SimulationEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        groupsRef(uid)
            .whereEqualTo("simulationId", entry.simulationId)
            .get()
            .addOnSuccessListener { snapshot ->
                val todosGrupos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SimulationGroup::class.java)?.apply { id = doc.id }
                }
                val proximaOrdem = (todosGrupos.maxOfOrNull { it.order } ?: 0) + 1
                criarGrupoEAdicionar(uid, entry, proximaOrdem, onSuccess, onError)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao verificar Grupos") }
    }

    private fun adicionarNoGrupo(
        uid: String,
        grupo: SimulationGroup,
        entry: SimulationEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val grupoRef = groupsRef(uid).document(grupo.id)
        val entryRef = entriesRef(uid).document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(grupoRef)
            val nodeCountAtual = (snapshot.getLong("nodeCount") ?: 0L).toInt()
            if (nodeCountAtual >= SimulationGroup.MAX_NODES_PER_GROUP) {
                throw IllegalStateException(GRUPO_CHEIO)
            }
            transaction.update(grupoRef, "nodeCount", nodeCountAtual + 1)
            transaction.set(entryRef, entry.copy(groupId = grupo.id))
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                if (e is IllegalStateException && e.message == GRUPO_CHEIO) {
                    // Alguém encheu o Grupo entre a consulta e a transação — tenta de
                    // novo do zero; dessa vez vai criar um Grupo novo.
                    createEntry(entry, onSuccess, onError)
                } else {
                    onError(e.message ?: "Erro ao adicionar lançamento ao Grupo")
                }
            }
    }

    private fun criarGrupoEAdicionar(
        uid: String,
        entry: SimulationEntry,
        ordem: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val grupo = SimulationGroup(
            simulationId = entry.simulationId,
            name = "Grupo $ordem",
            order = ordem,
            bankId = entry.bankId,
            bankName = entry.bankName,
            cardId = entry.cardId,
            cardLabel = entry.cardLabel,
            entryType = entry.type,
            paymentMethod = entry.paymentMethod ?: PaymentMethod.DEBIT,
            nodeCount = 1
        )
        val grupoRef = groupsRef(uid).document()
        val entryRef = entriesRef(uid).document()

        db.runTransaction { transaction ->
            transaction.set(grupoRef, grupo)
            transaction.set(entryRef, entry.copy(groupId = grupoRef.id))
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao criar Grupo") }
    }

    /**
     * Renomeia um Grupo
     */
    fun renameGroup(groupId: String, novoNome: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        groupsRef(uid).document(groupId)
            .update("name", novoNome)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear o Grupo") }
    }

    /** Renomeia um lançamento (permitido pra compra e economia)*/
    fun updateEntryTitle(entry: SimulationEntry, novoTitulo: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        entriesRef(uid).document(entry.id)
            .update("title", novoTitulo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear lançamento") }
    }

    /**
     * Atualiza a meta e o período (início/fim) de uma economia já criada
     */
    fun updateSavingsGoal(
        entry: SimulationEntry,
        novaMeta: Double,
        novoInicio: Long,
        novoFim: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val zoneId = ZoneId.systemDefault()
        val fimDate = Instant.ofEpochMilli(novoFim).atZone(zoneId).toLocalDate()
        val novoValorMensal = SimulationCalculator.calcularValorMensalEconomia(novaMeta, LocalDate.now(), fimDate)

        entriesRef(uid).document(entry.id)
            .update(
                mapOf(
                    "targetValue" to novaMeta,
                    "startDate" to novoInicio,
                    "endDate" to novoFim,
                    "monthlyAmount" to novoValorMensal
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar meta da economia") }
    }

    /**
     * Exclui um lançamento. O Grupo dele perde um nó; se ficar sem nenhum nó, o Grupo
     * inteiro é excluído junto (evita cluster fantasma vazio no canvas)
     */
    fun deleteEntry(entry: SimulationEntry, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val groupRef = groupsRef(uid).document(entry.groupId)
        val entryRef = entriesRef(uid).document(entry.id)

        db.runTransaction { transaction ->
            val groupSnapshot = transaction.get(groupRef)
            val countAtual = (groupSnapshot.getLong("nodeCount") ?: 1L).toInt()
            val novoCount = (countAtual - 1).coerceAtLeast(0)
            if (novoCount <= 0) {
                transaction.delete(groupRef)
            } else {
                transaction.update(groupRef, "nodeCount", novoCount)
            }
            transaction.delete(entryRef)
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir lançamento") }
    }

    /**
     * Move um lançamento pra outro Grupo (arrastar-e-soltar no canvas). Só deve ser
     * chamado com um Grupo de destino já validado como compatível (mesmo banco/cartão/
     * tipo) e com vaga
     */
    fun moveEntryToGroup(
        entry: SimulationEntry,
        toGroup: SimulationGroup,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        if (entry.groupId == toGroup.id) {
            onSuccess()
            return
        }
        val fromGroupRef = groupsRef(uid).document(entry.groupId)
        val toGroupRef = groupsRef(uid).document(toGroup.id)
        val entryRef = entriesRef(uid).document(entry.id)

        db.runTransaction { transaction ->
            val fromSnapshot = transaction.get(fromGroupRef)
            val toSnapshot = transaction.get(toGroupRef)

            val toCountAtual = (toSnapshot.getLong("nodeCount") ?: 0L).toInt()
            if (toCountAtual >= SimulationGroup.MAX_NODES_PER_GROUP) {
                throw IllegalStateException(GRUPO_CHEIO)
            }

            val fromCountAtual = (fromSnapshot.getLong("nodeCount") ?: 1L).toInt()
            val novoFromCount = (fromCountAtual - 1).coerceAtLeast(0)
            if (novoFromCount <= 0) {
                transaction.delete(fromGroupRef)
            } else {
                transaction.update(fromGroupRef, "nodeCount", novoFromCount)
            }
            transaction.update(toGroupRef, "nodeCount", toCountAtual + 1)
            transaction.update(entryRef, "groupId", toGroup.id)
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                if (e is IllegalStateException && e.message == GRUPO_CHEIO) {
                    onError("Esse Grupo já está cheio")
                } else {
                    onError(e.message ?: "Erro ao mover lançamento")
                }
            }
    }

    fun loadActiveCreditPurchases(onResult: (List<SimulationEntry>) -> Unit) {
        simulationRepository.loadSimulations(
            onSuccess = { simulations ->
                val ativas = simulations.filter { it.active }
                if (ativas.isEmpty()) {
                    onResult(emptyList())
                    return@loadSimulations
                }

                val todas = mutableListOf<SimulationEntry>()
                var restantes = ativas.size
                ativas.forEach { simulation ->
                    loadEntries(
                        simulationId = simulation.id,
                        onSuccess = { entries ->
                            todas.addAll(
                                entries.filter {
                                    it.type == SimulationEntryType.PURCHASE && it.paymentMethod == PaymentMethod.CREDIT
                                }
                            )
                            restantes--
                            if (restantes == 0) onResult(todas)
                        },
                        onError = {
                            restantes--
                            if (restantes == 0) onResult(todas)
                        }
                    )
                }
            },
            onError = { onResult(emptyList()) }
        )
    }

    /**
     * Apaga todos os lançamentos e Grupos de um banco ou, se cardId for informado, só
     * os de um cartão específico desse banco
     */
    fun deleteEntriesAndGroupsForBank(
        bankId: String,
        cardId: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        var entriesQuery = entriesRef(uid).whereEqualTo("bankId", bankId)
        var groupsQuery = groupsRef(uid).whereEqualTo("bankId", bankId)
        if (cardId != null) {
            entriesQuery = entriesQuery.whereEqualTo("cardId", cardId)
            groupsQuery = groupsQuery.whereEqualTo("cardId", cardId)
        }

        entriesQuery.get()
            .addOnSuccessListener { entriesSnapshot ->
                groupsQuery.get()
                    .addOnSuccessListener { groupsSnapshot ->
                        val referencias = entriesSnapshot.documents.map { it.reference } +
                            groupsSnapshot.documents.map { it.reference }
                        apagarEmLotes(referencias, onSuccess, onError)
                    }
                    .addOnFailureListener { onError(it.message ?: "Erro ao apagar Grupos relacionados") }
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao apagar lançamentos relacionados") }
    }

    // Firestore limita 500 operações por batch, corta em lotes de 400 (com folga) e
    // confirma um de cada vez, em sequência, só chamando onSuccess quando o último lote
    // terminar. Mesmo padrão já usado em `StatementTransactionRepository.apagarEmLotes`
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
            .addOnFailureListener { onError(it.message ?: "Erro ao apagar dados relacionados") }
    }

    private companion object {
        const val GRUPO_CHEIO = "GRUPO_CHEIO"
    }
}
