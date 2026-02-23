package avinash.app.audiocallapp.data.repository

import android.util.Log
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.domain.repository.UserPresenceRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPresenceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserPresenceRepository {

    companion object {
        private const val TAG = "UserPresenceRepo"
    }

    private val usersCollection = firestore.collection("users")

    override fun observeAvailableUsers(currentUserId: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing users: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { data -> User.fromMap(data, doc.id) }
                }?.filter { it.uniqueId != currentUserId } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }
}
