package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun checkAvailability(uniqueId: String): Boolean {
        if (uniqueId.length < 3) return false
        if (!uniqueId.matches(Regex("^[a-zA-Z0-9_]+$"))) return false
        return authRepository.checkUniqueIdAvailability(uniqueId)
    }

    suspend fun register(uniqueId: String, displayName: String): Result<User> {
        if (uniqueId.length < 3) {
            return Result.failure(Exception("Username must be at least 3 characters"))
        }
        if (!uniqueId.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return Result.failure(Exception("Username can only contain letters, numbers, and underscores"))
        }
        return authRepository.registerUser(uniqueId, displayName)
    }
}
