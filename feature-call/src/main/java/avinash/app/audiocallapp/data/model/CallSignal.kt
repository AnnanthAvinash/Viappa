package avinash.app.audiocallapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CallSignal(
    @DocumentId
    val callId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val calleeName: String = "",
    val status: CallStatus = CallStatus.RINGING,
    val offer: SessionDescription? = null,
    val answer: SessionDescription? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    constructor() : this("", "", "", "", "", CallStatus.RINGING, null, null, null)

    fun toMap(): Map<String, Any?> = mapOf(
        "callerId" to callerId,
        "calleeId" to calleeId,
        "callerName" to callerName,
        "calleeName" to calleeName,
        "status" to status.name,
        "offer" to offer?.toMap(),
        "answer" to answer?.toMap(),
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): CallSignal {
            @Suppress("UNCHECKED_CAST")
            return CallSignal(
                callId = id,
                callerId = map["callerId"] as? String ?: "",
                calleeId = map["calleeId"] as? String ?: "",
                callerName = map["callerName"] as? String ?: "",
                calleeName = map["calleeName"] as? String ?: "",
                status = try {
                    CallStatus.valueOf(map["status"] as? String ?: "RINGING")
                } catch (e: Exception) {
                    CallStatus.RINGING
                },
                offer = (map["offer"] as? Map<String, Any?>)?.let { SessionDescription.fromMap(it) },
                answer = (map["answer"] as? Map<String, Any?>)?.let { SessionDescription.fromMap(it) },
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}

enum class CallStatus {
    RINGING,
    ACCEPTED,
    REJECTED,
    ENDED
}
