package com.luamuniz.dinlux.export

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.excerpt.StatementTransactionRecord
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.list.ShoppingList
import com.luamuniz.dinlux.list.ShoppingListItem
import com.luamuniz.dinlux.simulation.Simulation
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationGroup

class ExportRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun carregarTudo(onSuccess: (DadosExportados) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)

        userRef.get()
            .addOnSuccessListener { userDoc ->
                val nome = userDoc.getString("nome") ?: ""
                val termsAcceptedAt = userDoc.getLong("termsAcceptedAt")
                val email = auth.currentUser?.email ?: ""

                var pendentes = 7
                var erroOcorrido = false
                var bancos = emptyList<Bank>()
                var simulacoes = emptyList<Simulation>()
                var lancamentos = emptyList<SimulationEntry>()
                var grupos = emptyList<SimulationGroup>()
                var extrato = emptyList<StatementTransactionRecord>()
                var listas = emptyList<ShoppingList>()
                var itensDeLista = emptyList<ShoppingListItem>()

                fun concluirParte() {
                    pendentes--
                    if (pendentes == 0 && !erroOcorrido) {
                        onSuccess(
                            DadosExportados(
                                nome = nome,
                                email = email,
                                termsAcceptedAt = termsAcceptedAt,
                                bancos = bancos,
                                simulacoes = simulacoes,
                                lancamentos = lancamentos,
                                grupos = grupos,
                                extrato = extrato,
                                listas = listas,
                                itensDeLista = itensDeLista
                            )
                        )
                    }
                }

                fun falhar(mensagem: String) {
                    if (!erroOcorrido) {
                        erroOcorrido = true
                        onError(mensagem)
                    }
                }

                userRef.collection(FirestoreCollections.BANKS).get()
                    .addOnSuccessListener { snapshot ->
                        bancos = snapshot.documents.mapNotNull { doc -> doc.toObject(Bank::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar bancos") }

                userRef.collection(FirestoreCollections.SIMULATIONS).get()
                    .addOnSuccessListener { snapshot ->
                        simulacoes = snapshot.documents.mapNotNull { doc -> doc.toObject(Simulation::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar simulações") }

                userRef.collection(FirestoreCollections.SIMULATION_ENTRIES).get()
                    .addOnSuccessListener { snapshot ->
                        lancamentos = snapshot.documents.mapNotNull { doc -> doc.toObject(SimulationEntry::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar lançamentos") }

                userRef.collection(FirestoreCollections.SIMULATION_GROUPS).get()
                    .addOnSuccessListener { snapshot ->
                        grupos = snapshot.documents.mapNotNull { doc -> doc.toObject(SimulationGroup::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar Grupos") }

                userRef.collection(FirestoreCollections.STATEMENT_TRANSACTIONS).get()
                    .addOnSuccessListener { snapshot ->
                        extrato = snapshot.documents.mapNotNull { doc -> doc.toObject(StatementTransactionRecord::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar extrato") }

                userRef.collection(FirestoreCollections.LISTS).get()
                    .addOnSuccessListener { snapshot ->
                        listas = snapshot.documents.mapNotNull { doc -> doc.toObject(ShoppingList::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar listas") }

                userRef.collection(FirestoreCollections.LIST_ITEMS).get()
                    .addOnSuccessListener { snapshot ->
                        itensDeLista = snapshot.documents.mapNotNull { doc -> doc.toObject(ShoppingListItem::class.java)?.apply { id = doc.id } }
                        concluirParte()
                    }
                    .addOnFailureListener { falhar(it.message ?: "Erro ao carregar itens de lista") }
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar perfil") }
    }
}
