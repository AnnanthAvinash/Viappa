package avinash.app.audiocallapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId
    val uniqueId: String = "",
    val displayName: String = "",
    val status: UserStatus = UserStatus.OFFLINE,
    @ServerTimestamp
    val lastSeen: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    // No-arg constructor for Firestore
    constructor() : this("", "", UserStatus.OFFLINE, null, null)

    fun toMap(): Map<String, Any?> = mapOf(
        "uniqueId" to uniqueId,
        "displayName" to displayName,
        "status" to status.name,
        "lastSeen" to lastSeen,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): User {
            return User(
                uniqueId = id,
                displayName = map["displayName"] as? String ?: "",
                status = try {
                    UserStatus.valueOf(map["status"] as? String ?: "OFFLINE")
                } catch (e: Exception) {
                    UserStatus.OFFLINE
                },
                lastSeen = map["lastSeen"] as? Timestamp,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}

enum class UserStatus {
    ONLINE,
    OFFLINE,
    IN_CALL
}
