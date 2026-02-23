package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserPresenceRepository {
    fun observeAvailableUsers(currentUserId: String): Flow<List<User>>
}
