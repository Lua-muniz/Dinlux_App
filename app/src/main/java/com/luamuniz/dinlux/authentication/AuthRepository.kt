package com.luamuniz.dinlux.authentication

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.core.TermosDePrivacidade

/**
 * Camada de acesso a dados (Model) da Autenticação.
 *
 * Nenhuma Activity deve chamar FirebaseAuth/FirebaseFirestore diretamente: toda a
 * comunicação com o Firebase para login, cadastro e recuperação de senha passa por aqui.
 * As Activities (View) só conversam com o AuthViewModel, que por sua vez usa este Repository.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun createUser(
        name: String,
        email: String,
        password: String,
        termsAccepted: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!termsAccepted) {
            onError("É necessário aceitar os Termos de Privacidade para criar sua conta")
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    onError("Não foi possível identificar o usuário criado")
                    return@addOnSuccessListener
                }
                saveUserProfile(uid, name, onSuccess, onError)
            }
            .addOnFailureListener { exception -> onError(mapAuthError(exception)) }
    }

    private fun saveUserProfile(
        uid: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val profile = mapOf(
            "nome" to name,
            "termsAccepted" to true,
            "termsAcceptedAt" to System.currentTimeMillis(),
            "termsVersion" to TermosDePrivacidade.VERSAO,
            "boasVindasExibida" to false
        )
        db.collection(FirestoreCollections.USERS).document(uid)
            .set(profile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError("Conta criada, mas houve um erro ao salvar o perfil") }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(mapAuthError(exception)) }
    }

    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Erro ao enviar o link de recuperação")
            }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun deleteAccount(
        currentPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onError("Usuário não autenticado")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                apagarDadosDoUsuario(
                    user = user,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
            .addOnFailureListener {
                onError("Senha atual incorreta")
            }
    }

    private fun apagarDadosDoUsuario(
        user: FirebaseUser,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = user.uid
        val subcolecoes = listOf(
            FirestoreCollections.BANKS,
            FirestoreCollections.SIMULATIONS,
            FirestoreCollections.SIMULATION_ENTRIES,
            FirestoreCollections.SIMULATION_GROUPS,
            FirestoreCollections.STATEMENT_TRANSACTIONS,
            FirestoreCollections.LISTS,
            FirestoreCollections.LIST_ITEMS
        )
        var restantes = subcolecoes.size
        var falhou = false

        subcolecoes.forEach { subcolecao ->
            db.collection(FirestoreCollections.USERS).document(uid).collection(subcolecao)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (falhou) return@addOnSuccessListener
                    val batch = db.batch()
                    snapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit()
                        .addOnSuccessListener {
                            restantes--
                            if (restantes == 0) {
                                apagarDocumentoUsuarioEContaAuth(user, onSuccess, onError)
                            }
                        }
                        .addOnFailureListener { exception ->
                            if (!falhou) {
                                falhou = true
                                onError(exception.message ?: "Erro ao apagar os dados da conta")
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    if (!falhou) {
                        falhou = true
                        onError(exception.message ?: "Erro ao apagar os dados da conta")
                    }
                }
        }
    }

    private fun apagarDocumentoUsuarioEContaAuth(
        user: FirebaseUser,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection(FirestoreCollections.USERS).document(user.uid)
            .delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { exception ->
                        onError(exception.message ?: "Dados apagados, mas houve um erro ao excluir a conta")
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Erro ao apagar os dados do usuário")
            }
    }

    private fun mapAuthError(exception: Exception): String = when (exception) {
        is FirebaseAuthWeakPasswordException -> "A senha deve ter no mínimo 6 caracteres"
        is FirebaseAuthUserCollisionException -> "Este e-mail já está cadastrado"
        is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha inválidos"
        else -> exception.message ?: "Erro ao processar a solicitação"
    }
}
