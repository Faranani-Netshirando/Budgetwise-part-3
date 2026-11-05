package com.example.budgetwisesa.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("budgetwise_preferences", Context.MODE_PRIVATE)
    val darkMode = MutableStateFlow(prefs.getBoolean("dark", false))
    val notifications = MutableStateFlow(prefs.getBoolean("notifications", false))
    val threshold = MutableStateFlow(prefs.getInt("threshold", 80))
    val language = MutableStateFlow(prefs.getString("language", java.util.Locale.getDefault().language.takeIf { it in listOf("en", "ve", "zu") } ?: "en") ?: "en")
    fun setDark(value: Boolean) { prefs.edit().putBoolean("dark", value).apply(); darkMode.value = value }
    fun setNotifications(value: Boolean) { prefs.edit().putBoolean("notifications", value).apply(); notifications.value = value }
    fun setThreshold(value: Int) { prefs.edit().putInt("threshold", value.coerceIn(50, 100)).apply(); threshold.value = value.coerceIn(50, 100) }
    fun setLanguage(value: String) { prefs.edit().putString("language", value).apply(); language.value = value }
}
