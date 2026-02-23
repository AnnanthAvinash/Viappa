package avinash.app.audiocallapp.data.model

import com.google.firebase.Timestamp

enum class WtConnectionStatus {
    OFFERING,
    CONNECTED,
    CLOSED
}

data class WtConnectionSignal(
    val pairId: String = "",
    val offererId: String = "",
    val answererId: String = "",
    val offererName: String = "",
    val answererName: String = "",
    val offer: SessionDescription? = null,
    val answer: SessionDescription? = null,
    val status: WtConnectionStatus = WtConnectionStatus.OFFERING,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "offererId" to offererId,
        "answererId" to answererId,
        "offererName" to offererName,
        "answererName" to answererName,
        "offer" to offer?.toMap(),
        "answer" to answer?.toMap(),
        "status" to status.name,
        "createdAt" to createdAt
    )

    companion object {
        fun createPairId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
        }

        fun isOfferer(myUserId: String, friendUserId: String): Boolean {
            return myUserId < friendUserId
        }

        fun fromMap(map: Map<String, Any?>, pairId: String): WtConnectionSignal {
            @Suppress("UNCHECKED_CAST")
            return WtConnectionSignal(
                pairId = pairId,
                offererId = map["offererId"] as? String ?: "",
                answererId = map["answererId"] as? String ?: "",
                offererName = map["offererName"] as? String ?: "",
                answererName = map["answererName"] as? String ?: "",
                offer = (map["offer"] as? Map<String, Any?>)?.let { SessionDescription.fromMap(it) },
                answer = (map["answer"] as? Map<String, Any?>)?.let { SessionDescription.fromMap(it) },
                status = try {
                    WtConnectionStatus.valueOf(map["status"] as? String ?: "OFFERING")
                } catch (e: Exception) {
                    WtConnectionStatus.OFFERING
                },
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
