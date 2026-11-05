package com.example.budgetwisesa.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.data.AppPreferences
import com.example.budgetwisesa.data.model.Budget
import java.time.LocalDate

object NotificationHelper {
    private const val CHANNEL = "budgetwise_updates"
    fun show(context: Context, id: Int, title: String, body: String): Boolean {
        if (!AppPreferences(context).notifications.value) return false
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notifications), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try {
            NotificationManagerCompat.from(context).notify(id, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body)
                .setContentIntent(open).setAutoCancel(true).build())
            true
        } catch (_: SecurityException) { false }
    }
    fun budgetAlert(context: Context, uid: String, budget: Budget, threshold: Int) {
        val today = LocalDate.now()
        val level = if (budget.spent >= budget.amount) 100 else threshold
        val prefs = context.getSharedPreferences("budgetwise_alerts", Context.MODE_PRIVATE)
        val key = "$uid-${budget.id}-$today-$level"
        if (prefs.getBoolean(key, false)) return
        if (show(context, key.hashCode(), context.getString(R.string.budget_alert),
            context.getString(R.string.budget_alert_body, budget.category, level))) {
            prefs.edit().putBoolean(key, true).apply()
        }
    }
}
