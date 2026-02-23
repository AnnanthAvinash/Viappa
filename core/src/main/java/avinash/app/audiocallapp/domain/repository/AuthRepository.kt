package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun checkUniqueIdAvailability(uniqueId: String): Boolean
    suspend fun registerUser(uniqueId: String, displayName: String): Result<User>
    suspend fun getCurrentUser(): User?
    suspend fun updateUserStatus(status: UserStatus)
    suspend fun updateDisplayName(name: String): Result<Unit>
    suspend fun signOut()
    fun observeCurrentUser(): Flow<User?>
    fun isUserLoggedIn(): Boolean
    fun getCurrentUserId(): String?
}
