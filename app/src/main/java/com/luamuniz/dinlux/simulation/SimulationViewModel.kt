package com.luamuniz.dinlux.simulation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.luamuniz.dinlux.finance.Bank

sealed class SimulationListUiState {
    object Loading : SimulationListUiState()
    data class Loaded(val simulations: List<Simulation>) : SimulationListUiState()
    data class Error(val message: String) : SimulationListUiState()
}

class SimulationViewModel(
    private val repository: SimulationRepository = SimulationRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<SimulationListUiState>(SimulationListUiState.Loading)
    val uiState: LiveData<SimulationListUiState> = _uiState

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    fun loadSimulations() {
        repository.loadSimulations(
            onSuccess = { _uiState.value = SimulationListUiState.Loaded(it) },
            onError = { _uiState.value = SimulationListUiState.Error(it) }
        )
    }

    fun createSimulation(title: String, active: Boolean = false) {
        repository.createSimulation(
            title = title,
            active = active,
            onSuccess = { loadSimulations() },
            onError = { _actionMessage.value = it }
        )
    }

    fun renameSimulation(id: String, newTitle: String) {
        repository.renameSimulation(
            id = id,
            newTitle = newTitle,
            onSuccess = { loadSimulations() },
            onError = { _actionMessage.value = it }
        )
    }

    /** Desativar não pede confirmação nem recalcula nada, só vira o flag */
    fun setActive(id: String, active: Boolean) {
        repository.setActive(
            id = id,
            active = active,
            onSuccess = { loadSimulations() },
            onError = { _actionMessage.value = it }
        )
    }

    /** Ativar recalcula a data de cada lançamento */
    fun activateSimulation(simulationId: String, entries: List<SimulationEntry>, banks: List<Bank>) {
        repository.activateSimulation(
            simulationId = simulationId,
            entries = entries,
            banks = banks,
            onSuccess = { loadSimulations() },
            onError = { _actionMessage.value = it }
        )
    }

    fun deleteSimulation(id: String) {
        repository.deleteSimulation(
            id = id,
            onSuccess = { loadSimulations() },
            onError = { _actionMessage.value = it }
        )
    }
}
