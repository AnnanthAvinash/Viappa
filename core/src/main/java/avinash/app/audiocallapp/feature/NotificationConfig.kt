package avinash.app.audiocallapp.feature

object NotificationConfig {
    const val CALL_CHANNEL_ID = "call_channel"
    const val WT_CHANNEL_ID = "wt_channel"

    @Volatile
    var notificationIconRes: Int = android.R.drawable.ic_menu_call
}
