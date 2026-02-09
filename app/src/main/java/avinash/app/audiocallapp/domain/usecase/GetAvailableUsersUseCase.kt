package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.SignalingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAvailableUsersUseCase @Inject constructor(
    private val signalingRepository: SignalingRepository
) {
    operator fun invoke(currentUserId: String): Flow<List<User>> {
        return signalingRepository.observeAvailableUsers(currentUserId)
    }
}
