package avinash.app.audiocallapp.data.model

data class IceCandidate(
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "candidate" to candidate,
        "sdpMid" to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): IceCandidate {
            return IceCandidate(
                candidate = map["candidate"] as? String ?: "",
                sdpMid = map["sdpMid"] as? String ?: "",
                sdpMLineIndex = (map["sdpMLineIndex"] as? Long)?.toInt() ?: 0
            )
        }
    }
}
