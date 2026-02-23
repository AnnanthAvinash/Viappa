package avinash.app.audiocallapp.data.model

import com.google.firebase.Timestamp

data class Connection(
    val id: String = "",
    val userAId: String = "",
    val userBId: String = "",
    val userAName: String = "",
    val userBName: String = "",
    val createdAt: Timestamp? = null
) {
    fun getFriendId(myId: String): String = if (userAId == myId) userBId else userAId
    fun getFriendName(myId: String): String = if (userAId == myId) userBName else userAName

    fun toMap(): Map<String, Any?> = mapOf(
        "userAId" to userAId,
        "userBId" to userBId,
        "userAName" to userAName,
        "userBName" to userBName,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Connection {
            return Connection(
                id = id,
                userAId = map["userAId"] as? String ?: "",
                userBId = map["userBId"] as? String ?: "",
                userAName = map["userAName"] as? String ?: "",
                userBName = map["userBName"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
