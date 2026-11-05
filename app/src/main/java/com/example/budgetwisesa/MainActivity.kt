package com.example.budgetwisesa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.budgetwisesa.data.AppPreferences
import com.example.budgetwisesa.databinding.ActivityMainBinding
import com.example.budgetwisesa.ui.views.*
import com.example.budgetwisesa.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class AppRoute(val title: Int, val menuId: Int = 0) {
    LOGIN(R.string.login), REGISTER(R.string.register), DASHBOARD(R.string.dashboard, R.id.nav_dashboard),
    TRANSACTIONS(R.string.transactions, R.id.nav_transactions), BUDGETS(R.string.budgets, R.id.nav_budgets),
    GOALS(R.string.goals, R.id.nav_goals), REPORTS(R.string.reports, R.id.nav_reports), SETTINGS(R.string.settings)
}
data class PhoneSession(var phoneMode: Boolean = false, var verificationId: String = "")

class MainActivity : AppCompatActivity() {
    val model: MainViewModel by viewModels()
    val phoneSession = PhoneSession()
    private lateinit var binding: ActivityMainBinding
    private var screen: NativeScreen? = null
    private var route = AppRoute.LOGIN
    private var receiptHandler: ((Uri?) -> Unit)? = null
    private var exportSource: String? = null

    private val googleResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            (screen as? LoginScreen)?.setBusy(false)
        } else try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(com.google.android.gms.common.api.ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(requireNotNull(account.idToken), null)
            FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
                (screen as? LoginScreen)?.setBusy(false)
                if (task.isSuccessful) rememberLoginMethod("Google") else authError()
            }
        } catch (_: Exception) { (screen as? LoginScreen)?.setBusy(false); authError() }
    }
    private val receiptPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        receiptHandler?.invoke(uri); receiptHandler = null
    }
    private val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { target ->
        val source = exportSource
        exportSource = null
        if (target != null && source != null) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val input = if (source.startsWith("https://")) java.net.URL(source).openConnection().apply {
                        connectTimeout = 15000; readTimeout = 15000
                    }.getInputStream() else contentResolver.openInputStream(Uri.parse(source))
                    requireNotNull(input).use { from ->
                        requireNotNull(contentResolver.openOutputStream(target)).use { to -> from.copyTo(to) }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message(R.string.export_failed) }
        }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        model.setNotifications(granted)
        if (!granted) message(R.string.notification_permission_denied)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = AppPreferences(this).darkMode.value
        val mode = if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        phoneSession.phoneMode = savedInstanceState?.getBoolean("phoneMode") ?: false
        phoneSession.verificationId = savedInstanceState?.getString("verificationId").orEmpty()
        exportSource = savedInstanceState?.getString("exportSource")
        val savedRoute = AppRoute.entries.firstOrNull { it.name == savedInstanceState?.getString("route") }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            AppRoute.entries.firstOrNull { it.menuId == item.itemId }?.let(::navigate)
            true
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) { navigate(AppRoute.SETTINGS); true } else false
        }
        navigate(savedRoute ?: if (model.isUserLoggedIn.value) AppRoute.DASHBOARD else AppRoute.LOGIN)
        onBackPressedDispatcher.addCallback(this) {
            when (route) {
                AppRoute.REGISTER -> navigate(AppRoute.LOGIN)
                AppRoute.SETTINGS, AppRoute.TRANSACTIONS, AppRoute.BUDGETS, AppRoute.GOALS, AppRoute.REPORTS -> navigate(AppRoute.DASHBOARD)
                else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.isUserLoggedIn.collect { loggedIn ->
                    if (loggedIn && route in listOf(AppRoute.LOGIN, AppRoute.REGISTER)) navigate(AppRoute.DASHBOARD)
                    else if (!loggedIn && route !in listOf(AppRoute.LOGIN, AppRoute.REGISTER)) navigate(AppRoute.LOGIN)
                } }
                launch { merge(model.transactions.map { Unit }, model.budgets.map { Unit }, model.goals.map { Unit },
                    model.userName.map { Unit }, model.isGuest.map { Unit }, model.alertThreshold.map { Unit },
                    model.notificationsEnabled.map { Unit }, model.language.map { Unit }, model.syncWork.map { Unit },
                    model.pendingCount.map { Unit }).collect { screen?.refresh() } }
                launch { model.isDarkMode.collect { value ->
                    val requested = if (value) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    if (AppCompatDelegate.getDefaultNightMode() != requested) AppCompatDelegate.setDefaultNightMode(requested)
                    screen?.refresh()
                } }
                launch { model.error.collect { id -> if (id != null) { message(id); model.clearError() } } }
            }
        }
    }

    fun navigate(requested: AppRoute) {
        val loggedIn = model.isUserLoggedIn.value
        route = when {
            !loggedIn && requested !in listOf(AppRoute.LOGIN, AppRoute.REGISTER) -> AppRoute.LOGIN
            loggedIn && requested in listOf(AppRoute.LOGIN, AppRoute.REGISTER) -> AppRoute.DASHBOARD
            else -> requested
        }
        screen?.close()
        screen = when (route) {
            AppRoute.LOGIN -> LoginScreen(this)
            AppRoute.REGISTER -> RegisterScreen(this)
            AppRoute.DASHBOARD -> DashboardScreen(this)
            AppRoute.TRANSACTIONS, AppRoute.BUDGETS, AppRoute.GOALS -> RecordsScreen(this, route)
            AppRoute.REPORTS -> ReportsScreen(this)
            AppRoute.SETTINGS -> SettingsScreen(this)
        }
        binding.screenHost.removeAllViews()
        binding.screenHost.addView(screen!!.root)
        binding.toolbar.isVisible = loggedIn
        binding.bottomNavigation.isVisible = loggedIn
        binding.toolbar.setTitle(route.title)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            val item = binding.bottomNavigation.menu.getItem(i)
            item.isChecked = item.itemId == route.menuId
        }
        screen?.refresh()
    }
    fun message(id: Int) { Snackbar.make(binding.root, id, Snackbar.LENGTH_LONG).show() }
    private fun authError() { (screen as? LoginScreen)?.showError(R.string.auth_failed) }
    fun rememberLoginMethod(method: String) { getSharedPreferences("login_prefs", MODE_PRIVATE).edit().putString("last_method", method).apply() }
    fun signInGoogle() {
        (screen as? LoginScreen)?.setBusy(true)
        try {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
            val client = GoogleSignIn.getClient(this, options)
            client.signOut().addOnCompleteListener { googleResult.launch(client.signInIntent) }
        } catch (_: Exception) { (screen as? LoginScreen)?.setBusy(false); authError() }
    }
    fun pickReceipt(callback: (Uri?) -> Unit) { receiptHandler = callback; receiptPicker.launch("image/*") }
    fun showReceipt(path: String) {
        val image = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (320 * resources.displayMetrics.density).toInt())
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = getString(R.string.receipt_image)
            load(path)
        }
        MaterialAlertDialogBuilder(this).setTitle(R.string.receipt_image).setView(image)
            .setPositiveButton(R.string.download_receipt) { _, _ -> exportSource = path; exportPicker.launch("BudgetWise-receipt") }
            .setNegativeButton(R.string.cancel, null).show()
    }
    fun enableNotifications(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else model.setNotifications(enabled)
    }
    override fun onResume() { super.onResume(); model.onResume() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("route", route.name)
        outState.putBoolean("phoneMode", phoneSession.phoneMode)
        outState.putString("verificationId", phoneSession.verificationId)
        outState.putString("exportSource", exportSource)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() { screen?.close(); receiptHandler = null; super.onDestroy() }
}
