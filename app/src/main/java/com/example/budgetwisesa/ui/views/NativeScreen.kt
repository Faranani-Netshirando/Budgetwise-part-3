package com.example.budgetwisesa.ui.views

import android.view.View
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

open class NativeScreen(val root: View) {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    protected var active = true
    open fun refresh() {}
    open fun close() { active = false; scope.cancel() }
}
fun number(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
fun money(value: Double): String = "R " + number(value)
fun date(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))
