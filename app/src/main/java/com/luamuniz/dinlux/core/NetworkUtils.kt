package com.luamuniz.dinlux.core

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.luamuniz.dinlux.DinluxApplication

/**
 * Checagem de conectividade usada SÓ nas poucas operações que dependem de rede
 * de verdade (Firebase Auth: login, criar conta, redefinir/alterar senha, alterar
 * e-mail, excluir conta) essas não têm fila offline como o Firestore, então travam
 * ou demoram muito pra falhar sem internet. O resto do app (Firestore) não usa isso:
 * fica por conta do cache/fila offline do próprio SDK
 */
object NetworkUtils {
    fun isOnline(): Boolean {
        val connectivityManager = DinluxApplication.instance
            .getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true // sem como checar, deixa a operação seguir e falhar (ou não) por conta própria

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    const val MENSAGEM_SEM_CONEXAO = "Sem conexão com a internet. Esta ação exige estar online."
}
