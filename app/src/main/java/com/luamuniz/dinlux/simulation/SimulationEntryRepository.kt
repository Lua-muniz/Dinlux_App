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

    // Usa batch (não transaction) com o nodeCount que `createEntry` já leu na consulta de
    // Grupos com vaga, em vez de reler o Grupo: `runTransaction` exige contato com o
    // servidor e falha offline, o que quebraria a criação de lançamento sem internet.
    // Custo: perde a proteção contra corrida (dois dispositivos enchendo o mesmo Grupo
    // ao mesmo tempo) — aceitável nesse app de usuário único, e inevitável de qualquer
    // forma quando offline (não dá pra confirmar o estado do servidor em tempo real)
    private fun adicionarNoGrupo(
        uid: String,
        grupo: SimulationGroup,
        entry: SimulationEntry,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val grupoRef = groupsRef(uid).document(grupo.id)
        val entryRef = entriesRef(uid).document()

        val batch = db.batch()
        batch.update(grupoRef, "nodeCount", grupo.nodeCount + 1)
        batch.set(entryRef, entry.copy(groupId = grupo.id))
        batch.commit()
            .addOnFailureListener { onError(it.message ?: "Erro ao adicionar lançamento ao Grupo") }
        onSuccess()
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

        val batch = db.batch()
        batch.set(grupoRef, grupo)
        batch.set(entryRef, entry.copy(groupId = grupoRef.id))
        batch.commit()
            .addOnFailureListener { onError(it.message ?: "Erro ao criar Grupo") }
        onSuccess()
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
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear o Grupo") }
        onSuccess()
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
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear lançamento") }
        onSuccess()
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
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar meta da economia") }
        onSuccess()
    }

    /**
     * Exclui um lançamento. O Grupo dele perde um nó; se ficar sem nenhum nó, o Grupo
     * inteiro é excluído junto (evita cluster fantasma vazio no canvas)
     */
    // Lê o Grupo em vez de usar runTransaction (que exige contato com o servidor e falha
    // offline): o `.get()` aqui resolve do cache local na hora, já que o Grupo com certeza
    // foi carregado antes pra aparecer no canvas de onde essa exclusão é chamada
    fun deleteEntry(entry: SimulationEntry, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val groupRef = groupsRef(uid).document(entry.groupId)
        val entryRef = entriesRef(uid).document(entry.id)

        groupRef.get()
            .addOnSuccessListener { groupSnapshot ->
                val countAtual = (groupSnapshot.getLong("nodeCount") ?: 1L).toInt()
                val novoCount = (countAtual - 1).coerceAtLeast(0)
                val batch = db.batch()
                if (novoCount <= 0) {
                    batch.delete(groupRef)
                } else {
                    batch.update(groupRef, "nodeCount", novoCount)
                }
                batch.delete(entryRef)
                batch.commit()
                    .addOnFailureListener { onError(it.message ?: "Erro ao excluir lançamento") }
                onSuccess()
            }
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
        if (toGroup.nodeCount >= SimulationGroup.MAX_NODES_PER_GROUP) {
            onError("Esse Grupo já está cheio")
            return
        }

        val fromGroupRef = groupsRef(uid).document(entry.groupId)
        val toGroupRef = groupsRef(uid).document(toGroup.id)
        val entryRef = entriesRef(uid).document(entry.id)

        // Só precisa reler o Grupo de origem: o de destino (toGroup) já veio atualizado
        // de quem chamou essa função. runTransaction foi trocado por get()+batch pelo
        // mesmo motivo de deleteEntry (transaction não funciona offline)
        fromGroupRef.get()
            .addOnSuccessListener { fromSnapshot ->
                val fromCountAtual = (fromSnapshot.getLong("nodeCount") ?: 1L).toInt()
                val novoFromCount = (fromCountAtual - 1).coerceAtLeast(0)
                val batch = db.batch()
                if (novoFromCount <= 0) {
                    batch.delete(fromGroupRef)
                } else {
                    batch.update(fromGroupRef, "nodeCount", novoFromCount)
                }
                batch.update(toGroupRef, "nodeCount", toGroup.nodeCount + 1)
                batch.update(entryRef, "groupId", toGroup.id)
                batch.commit()
                    .addOnFailureListener { onError(it.message ?: "Erro ao mover lançamento") }
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao mover lançamento") }
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
    // dispara todos de uma vez (cada um entra no cache/fila local na hora, offline ou
    // não — não precisa esperar um terminar pra começar o próximo). Mesmo padrão usado
    // em `StatementTransactionRepository.apagarEmLotes`
    private fun apagarEmLotes(
        referencias: List<DocumentReference>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (referencias.isEmpty()) {
            onSuccess()
            return
        }
        var falhou = false
        referencias.chunked(400).forEach { lote ->
            val batch = db.batch()
            lote.forEach { batch.delete(it) }
            batch.commit()
                .addOnFailureListener {
                    if (!falhou) {
                        falhou = true
                        onError(it.message ?: "Erro ao apagar dados relacionados")
                    }
                }
        }
        onSuccess()
    }
}
