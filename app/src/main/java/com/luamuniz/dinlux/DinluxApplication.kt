package com.luamuniz.dinlux

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.firestore.firestoreSettings

/**
 * Configura explicitamente o cache local do Firestore como persistente e sem limite de
 * tamanho (o padrão do SDK já vem com persistência ligada, mas com um teto de ~100MB
 * que pode acabar descartando dados salvos offline num app financeiro que guarda
 * extrato, simulações etc.). É essa configuração que permite ler/editar tudo com o
 * app offline e sincronizar depois, sozinho, quando a conexão voltar
 */
class DinluxApplication : Application() {

    companion object {
        lateinit var instance: DinluxApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                }
            )
        }
    }
}
