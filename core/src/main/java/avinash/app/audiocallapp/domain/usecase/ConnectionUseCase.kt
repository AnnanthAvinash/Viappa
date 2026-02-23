package avinash.app.audiocallapp.domain.usecase

import avinash.app.audiocallapp.data.model.Connection
import avinash.app.audiocallapp.data.model.ConnectionRequest
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConnectionUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    fun searchUsers(query: String, currentUserId: String): Flow<List<User>> {
        return connectionRepository.searchUsers(query, currentUserId)
    }

    suspend fun sendRequest(fromId: String, fromName: String, toId: String, toName: String): Result<Unit> {
        if (fromId == toId) return Result.failure(Exception("Cannot send request to yourself"))
        return connectionRepository.sendRequest(fromId, fromName, toId, toName)
    }

    suspend fun acceptRequest(request: ConnectionRequest): Result<Unit> {
        return connectionRepository.acceptRequest(request)
    }

    suspend fun rejectRequest(requestId: String): Result<Unit> {
        return connectionRepository.rejectRequest(requestId)
    }

    suspend fun cancelRequest(requestId: String): Result<Unit> {
        return connectionRepository.cancelRequest(requestId)
    }

    suspend fun removeFriend(connectionId: String): Result<Unit> {
        return connectionRepository.removeFriend(connectionId)
    }

    fun observeReceivedRequests(myId: String): Flow<List<ConnectionRequest>> {
        return connectionRepository.observeReceivedRequests(myId)
    }

    fun observeSentRequests(myId: String): Flow<List<ConnectionRequest>> {
        return connectionRepository.observeSentRequests(myId)
    }

    fun observeFriends(myId: String): Flow<List<Connection>> {
        return connectionRepository.observeFriends(myId)
    }
}
