package avinash.app.audiocallapp.presentation.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.data.repository.CallHistoryRepository
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.GetAvailableUsersUseCase
import avinash.app.audiocallapp.feature.FeatureInitializer
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
    val errorMessage: String? = null
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getAvailableUsersUseCase: GetAvailableUsersUseCase,
    private val featureInitializers: Set<@JvmSuppressWildcards FeatureInitializer>,
    val callHistoryRepository: CallHistoryRepository
) : ViewModel() {

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
                authRepository.updateUserStatus(UserStatus.ONLINE)
                observeAvailableUsers(user.uniqueId)
                featureInitializers.forEach { it.initialize(user.uniqueId, user.displayName) }
            }
        }
    }

    private fun observeAvailableUsers(currentUserId: String) {
        viewModelScope.launch {
            getAvailableUsersUseCase(currentUserId)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load users")
                    }
                }
                .collect { users ->
                    _uiState.update { it.copy(availableUsers = users, isLoading = false) }
                }
        }
    }

    fun updateDisplayName(name: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.updateDisplayName(name)
            onResult(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            featureInitializers.forEach { it.cleanup() }
            authRepository.signOut()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            featureInitializers.forEach { it.cleanup() }
            authRepository.updateUserStatus(UserStatus.OFFLINE)
        }
    }
}
