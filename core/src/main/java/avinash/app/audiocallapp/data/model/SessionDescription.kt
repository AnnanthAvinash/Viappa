package avinash.app.audiocallapp.data.model

data class SessionDescription(
    val type: String = "",
    val sdp: String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "type" to type,
        "sdp" to sdp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): SessionDescription {
            return SessionDescription(
                type = map["type"] as? String ?: "",
                sdp = map["sdp"] as? String ?: ""
            )
        }
    }
}
