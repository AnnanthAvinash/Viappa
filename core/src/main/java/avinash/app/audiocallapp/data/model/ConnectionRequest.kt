package avinash.app.audiocallapp.data.model

import com.google.firebase.Timestamp

data class ConnectionRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val receiverName: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "senderId" to senderId,
        "receiverId" to receiverId,
        "senderName" to senderName,
        "receiverName" to receiverName,
        "status" to status,
        "createdAt" to createdAt
    )

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_REJECTED = "REJECTED"

        fun fromMap(map: Map<String, Any?>, id: String): ConnectionRequest {
            return ConnectionRequest(
                id = id,
                senderId = map["senderId"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                receiverName = map["receiverName"] as? String ?: "",
                status = map["status"] as? String ?: STATUS_PENDING,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
