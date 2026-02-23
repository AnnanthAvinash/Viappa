package avinash.app.audiocallapp.domain.repository

import avinash.app.audiocallapp.data.model.Connection
import avinash.app.audiocallapp.data.model.ConnectionRequest
import avinash.app.audiocallapp.data.model.User
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun searchUsers(query: String, currentUserId: String): Flow<List<User>>
    suspend fun sendRequest(fromId: String, fromName: String, toId: String, toName: String): Result<Unit>
    suspend fun acceptRequest(request: ConnectionRequest): Result<Unit>
    suspend fun rejectRequest(requestId: String): Result<Unit>
    suspend fun cancelRequest(requestId: String): Result<Unit>
    suspend fun removeFriend(connectionId: String): Result<Unit>
    fun observeReceivedRequests(myId: String): Flow<List<ConnectionRequest>>
    fun observeSentRequests(myId: String): Flow<List<ConnectionRequest>>
    fun observeFriends(myId: String): Flow<List<Connection>>
}
