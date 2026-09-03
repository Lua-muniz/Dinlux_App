package com.luamuniz.dinlux.list

/**
 * Uma lista, o "projeto" nomeado pelo usuário: só um contêiner com nome, os itens
 * de verdade ficam em ShoppingListItem, ligados pelo campo ShoppingListItem.listId
 */
data class ShoppingList(
    var id: String = "",
    var title: String = "",
    var createdAt: Long = System.currentTimeMillis()
)
