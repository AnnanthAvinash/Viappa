package avinash.app.audiocallapp.presentation.userlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.model.CallSignal
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.CallUseCase
import avinash.app.audiocallapp.domain.usecase.GetAvailableUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserListUiState(
    val currentUser: User? = null,
    val availableUsers: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val incomingCall: CallSignal? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getAvailableUsersUseCase: GetAvailableUsersUseCase,
    private val callUseCase: CallUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "AudioCallApp"
    }

    private val _uiState = MutableStateFlow(UserListUiState())
    val uiState: StateFlow<UserListUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update { it.copy(currentUser = user) }
                
                // Set user online
                authRepository.updateUserStatus(UserStatus.ONLINE)
                
                // Observe available users
                observeAvailableUsers(user.uniqueId)
                
                // Observe incoming calls
                observeIncomingCalls(user.uniqueId)
            }
        }
    }

    private fun observeAvailableUsers(currentUserId: String) {
        viewModelScope.launch {
            getAvailableUsersUseCase(currentUserId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load users"
                        )
                    }
                }
                .collect { users ->
                    _uiState.update {
                        it.copy(
                            availableUsers = users,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeIncomingCalls(calleeId: String) {
        viewModelScope.launch {
            Log.d(TAG, "[INCOMING] Starting listener for: $calleeId")
            callUseCase.observeIncomingCalls(calleeId)
                .catch { e ->
                    Log.e(TAG, "[INCOMING] Error: ${e.message}")
                }
                .collect { call ->
                    if (call != null) {
                        Log.d(TAG, "[INCOMING] Call notification: ${call.callerName}")
                    }
                    _uiState.update { it.copy(incomingCall = call) }
                }
        }
    }

    fun rejectIncomingCall() {
        viewModelScope.launch {
            val callId = _uiState.value.incomingCall?.callId ?: return@launch
            Log.d(TAG, "[INCOMING] Rejecting call: $callId")
            callUseCase.rejectCall(callId)
            _uiState.update { it.copy(incomingCall = null) }
            Log.d(TAG, "[INCOMING] Call rejected")
        }
    }

    fun clearIncomingCall() {
        _uiState.update { it.copy(incomingCall = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            authRepository.updateUserStatus(UserStatus.OFFLINE)
        }
    }
}
