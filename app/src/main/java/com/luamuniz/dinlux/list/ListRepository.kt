package com.luamuniz.dinlux.list

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections

/**
 * Acesso a dados (Model) do módulo de Lista.
 */
class ListRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun listsRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.LISTS)

    private fun itemsRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.LIST_ITEMS)

    fun loadLists(onSuccess: (List<ShoppingList>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        listsRef(uid).get()
            .addOnSuccessListener { snapshot ->
                val listas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShoppingList::class.java)?.apply { id = doc.id }
                }.sortedByDescending { it.createdAt }
                onSuccess(listas)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar as listas") }
    }

    fun createList(title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        listsRef(uid).add(ShoppingList(title = title))
            .addOnFailureListener { onError(it.message ?: "Erro ao criar a lista") }
        // Dispara onSuccess assim que a escrita entra no cache local (offline ou não):
        // sincroniza sozinha quando conectar, sem travar a tela esperando o servidor
        onSuccess()
    }

    fun renameList(id: String, newTitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        listsRef(uid).document(id).update("title", newTitle)
            .addOnFailureListener { onError(it.message ?: "Erro ao renomear a lista") }
        onSuccess()
    }

    fun deleteList(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        itemsRef(uid).whereEqualTo("listId", id).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(listsRef(uid).document(id))
                batch.commit()
                    .addOnFailureListener { onError(it.message ?: "Erro ao excluir a lista") }
                onSuccess()
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir a lista") }
    }

    fun loadItems(listId: String, onSuccess: (List<ShoppingListItem>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        itemsRef(uid).whereEqualTo("listId", listId).get()
            .addOnSuccessListener { snapshot ->
                val itens = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShoppingListItem::class.java)?.apply { id = doc.id }
                }.sortedBy { it.createdAt }
                onSuccess(itens)
            }
            .addOnFailureListener { onError(it.message ?: "Erro ao carregar os itens") }
    }

    fun addItem(
        listId: String,
        name: String,
        quantity: Double?,
        price: Double?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        val item = ShoppingListItem(listId = listId, name = name, quantity = quantity, price = price)
        itemsRef(uid).add(item)
            .addOnFailureListener { onError(it.message ?: "Erro ao adicionar o item") }
        onSuccess()
    }

    fun updateItem(
        itemId: String,
        name: String,
        quantity: Double?,
        price: Double?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        itemsRef(uid).document(itemId)
            .update("name", name, "quantity", quantity, "price", price)
            .addOnFailureListener { onError(it.message ?: "Erro ao editar o item") }
        onSuccess()
    }

    fun setItemDone(itemId: String, done: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        itemsRef(uid).document(itemId).update("done", done)
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar o item") }
        onSuccess()
    }

    fun deleteItem(itemId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }
        itemsRef(uid).document(itemId).delete()
            .addOnFailureListener { onError(it.message ?: "Erro ao excluir o item") }
        onSuccess()
    }
}
