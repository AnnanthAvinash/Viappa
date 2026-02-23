package avinash.app.audiocallapp.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity
import avinash.app.audiocallapp.domain.usecase.CallHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallHistoryUiState(
    val calls: List<CallHistoryEntity> = emptyList(),
    val missedCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CallHistoryViewModel @Inject constructor(
    private val callHistoryUseCase: CallHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallHistoryUiState())
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    private var currentFilter: String? = null

    init {
        loadHistory(null)
        observeMissedCount()
    }

    fun loadHistory(typeFilter: String?) {
        currentFilter = typeFilter
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            callHistoryUseCase.getCallHistory(typeFilter)
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { calls -> _uiState.update { it.copy(calls = calls, isLoading = false) } }
        }
    }

    private fun observeMissedCount() {
        viewModelScope.launch {
            callHistoryUseCase.getMissedCallCount()
                .collect { count -> _uiState.update { it.copy(missedCount = count) } }
        }
    }

    fun markMissedSeen() {
        viewModelScope.launch {
            callHistoryUseCase.markMissedCallsSeen()
        }
    }

    suspend fun getCallsWithUser(userId: String) = callHistoryUseCase.getCallsWithUser(userId)
}
