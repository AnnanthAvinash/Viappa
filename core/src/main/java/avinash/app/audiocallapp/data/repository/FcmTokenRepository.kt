package avinash.app.audiocallapp.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FcmTokenRepo"
    }

    suspend fun saveCurrentToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenToFirestore(token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
        }
    }

    suspend fun saveTokenToFirestore(token: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val mappingDoc = firestore.collection("uid_mapping").document(uid).get().await()
            val uniqueId = mappingDoc.getString("uniqueId") ?: return
            firestore.collection("users").document(uniqueId)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
                .await()
            Log.d(TAG, "FCM token saved for $uniqueId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }

    suspend fun clearToken() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            val mappingDoc = firestore.collection("uid_mapping").document(uid).get().await()
            val uniqueId = mappingDoc.getString("uniqueId") ?: return
            firestore.collection("users").document(uniqueId)
                .update("fcmToken", "")
                .await()
        } catch (_: Exception) {}
    }
}
