package com.luamuniz.dinlux.list

/**
 * Um item dentro de uma ShoppingList funciona tanto como item de lista de compras
 * quanto como item simples de "a fazer" (só o nome)
 *
 * quantity e price são opcionais (`null` = não preenchido pelo usuário) a tela só
 * mostra o que foi preenchido, e o total da lista soma apenas os itens que têm preço definido
 */
data class ShoppingListItem(
    var id: String = "",
    var listId: String = "",
    var name: String = "",
    var quantity: Double? = null,
    var price: Double? = null,
    var done: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
)
