package com.example.budgetwisesa.viewmodel

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.budgetwisesa.R
import com.example.budgetwisesa.data.AppPreferences
import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.data.repository.BudgetRepository
import com.example.budgetwisesa.domain.FinanceRules
import com.example.budgetwisesa.notifications.NotificationHelper
import com.example.budgetwisesa.notifications.PushRegistration
import com.example.budgetwisesa.sync.SyncWorker
import com.google.firebase.auth.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val preferences = AppPreferences(application)
    private val repository = MutableStateFlow<BudgetRepository?>(null)
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()
    private val _isGuest = MutableStateFlow(false)
    val isGuest = _isGuest.asStateFlow()
    val isDarkMode = preferences.darkMode.asStateFlow()
    val notificationsEnabled = preferences.notifications.asStateFlow()
    val alertThreshold = preferences.threshold.asStateFlow()
    val language = preferences.language.asStateFlow()
    private val _error = MutableStateFlow<Int?>(null)
    val error = _error.asStateFlow()

    private data class LocalData(val account: String? = null, val transactions: List<Transaction> = emptyList(),
        val budgets: List<Budget> = emptyList(), val goals: List<SavingsGoal> = emptyList())
    private val localData = repository.flatMapLatest { repo ->
        if (repo == null) flowOf(LocalData()) else combine(repo.allTransactions, repo.allBudgets, repo.allGoals) { t, b, g ->
            LocalData(repo.account, t, b, g)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalData())
    val transactions = localData.map { it.transactions }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val periodClock = MutableStateFlow(System.currentTimeMillis())
    val budgets = combine(localData, periodClock) { data, now ->
        data.budgets.map { it.copy(spent = FinanceRules.spending(it, data.transactions, now)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = localData.map { it.goals }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingCount = repository.flatMapLatest { it?.pendingCount ?: flowOf(0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val syncWork = repository.flatMapLatest { repo ->
        if (repo == null || repo.account == BudgetRepository.GUEST) flowOf(emptyList())
        else WorkManager.getInstance(application).getWorkInfosForUniqueWorkFlow(SyncWorker.name(repo.account))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val previous = repository.value?.account
        val next = user?.uid
        if (previous != next && previous != null && previous != BudgetRepository.GUEST) SyncWorker.cancel(application, previous)
        repository.value = next?.let { BudgetRepository(application, it) }
        _isGuest.value = false
        _userName.value = user?.displayName ?: user?.email.orEmpty()
        _isUserLoggedIn.value = user != null
        if (next != null) {
            SyncWorker.enqueue(application, next)
            if (notificationsEnabled.value) PushRegistration.register()
        }
    }

    init {
        auth.addAuthStateListener(authListener)
        viewModelScope.launch {
            combine(localData, alertThreshold, notificationsEnabled, periodClock) { data, threshold, enabled, now -> Triple(data, threshold, if (enabled) now else null) }
                .collect { (data, threshold, now) ->
                    val uid = data.account ?: return@collect
                    if (now != null && repository.value?.account == uid) data.budgets.map {
                        it.copy(spent = FinanceRules.spending(it, data.transactions, now))
                    }.filter { it.spent >= it.amount * threshold / 100.0 }.forEach {
                        NotificationHelper.budgetAlert(application, uid, it, threshold)
                    }
                }
        }
    }

    fun toggleDarkMode(enabled: Boolean) = preferences.setDark(enabled)
    fun setThreshold(value: Int) = preferences.setThreshold(value)
    fun setNotifications(enabled: Boolean) {
        preferences.setNotifications(enabled)
        if (enabled) PushRegistration.register() else PushRegistration.unregister()
    }
    fun setLanguage(code: String) {
        if (code !in listOf("en", "ve", "zu")) return
        preferences.setLanguage(code)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }
    fun clearError() { _error.value = null }
    fun refreshProfile() { _userName.value = auth.currentUser?.displayName ?: auth.currentUser?.email.orEmpty() }
    fun logout() {
        repository.value?.account?.takeIf { it != BudgetRepository.GUEST }?.let { SyncWorker.cancel(getApplication(), it) }
        PushRegistration.unregister()
        repository.value = null
        _isGuest.value = false
        _isUserLoggedIn.value = false
        auth.signOut()
    }
    fun skipLogin() {
        repository.value = BudgetRepository(getApplication(), BudgetRepository.GUEST)
        _isGuest.value = true
        _isUserLoggedIn.value = true
        _userName.value = getApplication<Application>().getString(R.string.local_mode)
    }
    fun syncNow() { repository.value?.account?.let { SyncWorker.enqueue(getApplication(), it) } }
    fun onResume() { periodClock.value = System.currentTimeMillis(); syncNow() }

    // Capture the repository at click time, so switching accounts cannot redirect a queued edit.
    private fun mutate(action: suspend (BudgetRepository) -> Unit) {
        val target = repository.value ?: return
        viewModelScope.launch {
            try { action(target); SyncWorker.enqueue(getApplication(), target.account) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _error.value = R.string.save_failed }
        }
    }
    fun addTransaction(item: Transaction) = mutate { it.save(item) }
    fun updateTransaction(item: Transaction) = mutate { it.save(item) }
    fun deleteTransaction(item: Transaction) = mutate { it.delete(item) }
    fun addBudget(item: Budget) = mutate { it.save(item) }
    fun updateBudget(item: Budget) = mutate { it.save(item) }
    fun deleteBudget(item: Budget) = mutate { it.delete(item) }
    fun addGoal(item: SavingsGoal) = mutate { it.save(item) }
    fun updateGoal(item: SavingsGoal) = mutate { it.save(item) }
    fun deleteGoal(item: SavingsGoal) = mutate { it.delete(item) }

    fun verifyPhoneNumber(number: String, activity: Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(auth).setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS).setActivity(activity).setCallbacks(callbacks).build())
    }
    override fun onCleared() { auth.removeAuthStateListener(authListener); super.onCleared() }
}
