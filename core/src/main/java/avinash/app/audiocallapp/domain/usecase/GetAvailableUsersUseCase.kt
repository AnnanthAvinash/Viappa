package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.UserPresenceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAvailableUsersUseCase @Inject constructor(
    private val userPresenceRepository: UserPresenceRepository
) {
    operator fun invoke(currentUserId: String): Flow<List<User>> {
        return userPresenceRepository.observeAvailableUsers(currentUserId)
    }
}
