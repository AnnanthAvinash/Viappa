package avinash.app.audiocallapp.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.usecase.RegisterUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegistrationUiState(
    val uniqueId: String = "",
    val displayName: String = "",
    val isCheckingAvailability: Boolean = false,
    val isAvailable: Boolean? = null,
    val isRegistering: Boolean = false,
    val registrationSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isAlreadyLoggedIn: Boolean = false,
    val currentUser: User? = null
)

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private var checkAvailabilityJob: Job? = null

    init {
        checkExistingUser()
    }

    private fun checkExistingUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        isAlreadyLoggedIn = true,
                        currentUser = user,
                        registrationSuccess = true
                    )
                }
            }
        }
    }

    fun onUniqueIdChange(newId: String) {
        val sanitizedId = newId.lowercase().take(20)
        _uiState.update {
            it.copy(
                uniqueId = sanitizedId,
                isAvailable = null,
                errorMessage = null
            )
        }

        // Debounce availability check
        checkAvailabilityJob?.cancel()
        if (sanitizedId.length >= 3) {
            checkAvailabilityJob = viewModelScope.launch {
                delay(500)
                checkAvailability(sanitizedId)
            }
        }
    }

    fun onDisplayNameChange(newName: String) {
        _uiState.update { it.copy(displayName = newName.take(30)) }
    }

    private suspend fun checkAvailability(uniqueId: String) {
        _uiState.update { it.copy(isCheckingAvailability = true) }
        try {
            val isAvailable = registerUserUseCase.checkAvailability(uniqueId)
            _uiState.update {
                it.copy(
                    isCheckingAvailability = false,
                    isAvailable = isAvailable
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isCheckingAvailability = false,
                    errorMessage = "Failed to check availability"
                )
            }
        }
    }

    fun register() {
        val currentState = _uiState.value
        if (currentState.uniqueId.length < 3) {
            _uiState.update { it.copy(errorMessage = "Username must be at least 3 characters") }
            return
        }
        if (currentState.isAvailable != true) {
            _uiState.update { it.copy(errorMessage = "Please choose an available username") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRegistering = true, errorMessage = null) }
            
            val result = registerUserUseCase.register(
                uniqueId = currentState.uniqueId,
                displayName = currentState.displayName.ifBlank { currentState.uniqueId }
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            registrationSuccess = true,
                            currentUser = user
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRegistering = false,
                            errorMessage = error.message ?: "Registration failed"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
