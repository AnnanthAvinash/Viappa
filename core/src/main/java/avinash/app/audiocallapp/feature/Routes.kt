package avinash.app.audiocallapp.feature

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val MAIN = "main"

    object Call {
        const val PATTERN = "call/{remoteUserId}/{remoteName}?isCaller={isCaller}"
        fun create(userId: String, name: String, isCaller: Boolean = true): String {
            val encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
            return "call/$userId/$encoded?isCaller=$isCaller"
        }
    }

    object CallFeedback {
        const val PATTERN = "call_feedback/{remoteName}/{duration}"
        fun create(remoteName: String, duration: String): String {
            val encoded = URLEncoder.encode(remoteName, StandardCharsets.UTF_8.toString())
            return "call_feedback/$encoded/$duration"
        }
    }

    object WalkieTalkie {
        const val PATTERN = "walkie_talkie/{friendId}/{friendName}"
        fun create(friendId: String, friendName: String): String {
            val encoded = URLEncoder.encode(friendName, StandardCharsets.UTF_8.toString())
            return "walkie_talkie/$friendId/$encoded"
        }
    }
}
