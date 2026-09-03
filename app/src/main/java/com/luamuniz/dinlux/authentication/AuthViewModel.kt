package com.luamuniz.dinlux.authentication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Estado da tela de autenticação (View State), observado pelas Activities.
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val uiState: LiveData<AuthUiState> = _uiState

    fun createUser(name: String, email: String, password: String, termsAccepted: Boolean) {
        _uiState.value = AuthUiState.Loading
        repository.createUser(
            name = name,
            email = email,
            password = password,
            termsAccepted = termsAccepted,
            onSuccess = { _uiState.value = AuthUiState.Success },
            onError = { message -> _uiState.value = AuthUiState.Error(message) }
        )
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        repository.login(
            email = email,
            password = password,
            onSuccess = { _uiState.value = AuthUiState.Success },
            onError = { message -> _uiState.value = AuthUiState.Error(message) }
        )
    }

    fun sendPasswordReset(email: String) {
        _uiState.value = AuthUiState.Loading
        repository.sendPasswordReset(
            email = email,
            onSuccess = { _uiState.value = AuthUiState.Success },
            onError = { message -> _uiState.value = AuthUiState.Error(message) }
        )
    }

    fun isUserLoggedIn(): Boolean = repository.isUserLoggedIn()
}
