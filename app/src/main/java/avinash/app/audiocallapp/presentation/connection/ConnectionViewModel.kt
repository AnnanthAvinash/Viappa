package avinash.app.audiocallapp.presentation.connection

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.model.Connection
import avinash.app.audiocallapp.data.model.ConnectionRequest
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.ConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionUiState(
    val searchResults: List<User> = emptyList(),
    val receivedRequests: List<ConnectionRequest> = emptyList(),
    val sentRequests: List<ConnectionRequest> = emptyList(),
    val friends: List<Connection> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = ""
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionUseCase: ConnectionUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ConnectionVM"
    }

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadCurrentUserAndObserve()
    }

    private fun loadCurrentUserAndObserve() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: run {
                Log.e(TAG, "User not authenticated")
                _uiState.update { it.copy(error = "Not authenticated", isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(currentUserId = user.uniqueId, currentUserName = user.displayName) }

            launch {
                connectionUseCase.observeReceivedRequests(user.uniqueId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { requests -> _uiState.update { it.copy(receivedRequests = requests) } }
            }
            launch {
                connectionUseCase.observeSentRequests(user.uniqueId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { requests -> _uiState.update { it.copy(sentRequests = requests) } }
            }
            launch {
                connectionUseCase.observeFriends(user.uniqueId)
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { friends -> _uiState.update { it.copy(friends = friends, isLoading = false) } }
            }
        }
    }

    fun searchUsers(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(300)
            connectionUseCase.searchUsers(query, _uiState.value.currentUserId)
                .catch { e -> _uiState.update { it.copy(error = e.message, isSearching = false) } }
                .collect { users -> _uiState.update { it.copy(searchResults = users, isSearching = false) } }
        }
    }

    fun sendRequest(toUserId: String, toUserName: String) {
        val state = _uiState.value
        if (state.currentUserId.isBlank()) {
            _uiState.update { it.copy(error = "Not authenticated yet. Please wait.") }
            return
        }
        viewModelScope.launch {
            val result = connectionUseCase.sendRequest(state.currentUserId, state.currentUserName, toUserId, toUserName)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Request sent to $toUserName")
                    _uiState.update { it.copy(successMessage = "Request sent to $toUserName") }
                },
                onFailure = { e ->
                    Log.e(TAG, "sendRequest failed: ${e.message}")
                    _uiState.update { it.copy(error = e.message ?: "Failed to send request") }
                }
            )
        }
    }

    fun acceptRequest(request: ConnectionRequest) {
        viewModelScope.launch {
            val result = connectionUseCase.acceptRequest(request)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "${request.senderName} is now your friend!") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to accept request") }
                }
            )
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            val result = connectionUseCase.rejectRequest(requestId)
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to reject request") }
            }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            val result = connectionUseCase.cancelRequest(requestId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(successMessage = "Request cancelled") } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Failed to cancel") } }
            )
        }
    }

    fun removeFriend(connectionId: String) {
        viewModelScope.launch {
            val result = connectionUseCase.removeFriend(connectionId)
            result.fold(
                onSuccess = { _uiState.update { it.copy(successMessage = "Friend removed") } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Failed to remove") } }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun isFriend(userId: String): Boolean {
        return _uiState.value.friends.any { it.userAId == userId || it.userBId == userId }
    }

    fun hasPendingRequest(userId: String): Boolean {
        return _uiState.value.sentRequests.any { it.receiverId == userId }
    }
}
