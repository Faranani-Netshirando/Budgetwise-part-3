package com.example.budgetwisesa.notifications

import com.example.budgetwisesa.data.AppPreferences
import com.example.budgetwisesa.sync.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Use data messages so account identity and the user's preference are checked in background too.
 * Reference: https://firebase.google.com/docs/cloud-messaging/android/receive-messages
 */
class BudgetMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (AppPreferences(this).notifications.value) PushRegistration.register()
    }
    override fun onMessageReceived(message: RemoteMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["uid"] != uid) return
        val event = message.data["eventId"] ?: message.messageId ?: return
        val prefs = getSharedPreferences("received_push", MODE_PRIVATE)
        val recent = prefs.getStringSet(uid, emptySet()).orEmpty()
        if (event in recent) return
        prefs.edit().putStringSet(uid, (recent.toList().takeLast(99) + event).toSet()).apply()
        SyncWorker.enqueue(this, uid)
        NotificationHelper.show(this, (message.messageId ?: message.sentTime.toString()).hashCode(),
            getString(com.example.budgetwisesa.R.string.app_name),
            getString(com.example.budgetwisesa.R.string.cloud_update))
    }
}
