package com.example.budgetwisesa.ui.views

import android.util.Patterns
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.budgetwisesa.AppRoute
import com.example.budgetwisesa.MainActivity
import com.example.budgetwisesa.R
import com.example.budgetwisesa.databinding.ScreenLoginBinding
import com.example.budgetwisesa.databinding.ScreenRegisterBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*

class LoginScreen(private val host: MainActivity,
    private val binding: ScreenLoginBinding = ScreenLoginBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    private val auth = FirebaseAuth.getInstance()
    private var busy = false
    init {
        val previous = host.getSharedPreferences("login_prefs", 0).getString("last_method", "Email").orEmpty()
        binding.previousMethod.text = host.getString(R.string.previous_method, previous)
        binding.google.setOnClickListener { host.signInGoogle() }
        binding.guest.setOnClickListener { host.model.skipLogin() }
        binding.register.setOnClickListener { host.navigate(AppRoute.REGISTER) }
        binding.toggleMethod.setOnClickListener {
            host.phoneSession.phoneMode = !host.phoneSession.phoneMode
            host.phoneSession.verificationId = ""
            updateMode()
        }
        listOf(binding.email, binding.password, binding.phone, binding.code).forEach { field ->
            field.doAfterTextChanged { binding.error.isVisible = false }
        }
        binding.login.setOnClickListener { signIn() }
        updateMode()
    }
    private fun updateMode() {
        val phone = host.phoneSession.phoneMode
        val code = host.phoneSession.verificationId.isNotEmpty()
        binding.emailLayout.isVisible = !phone
        binding.passwordLayout.isVisible = !phone
        binding.phoneLayout.isVisible = phone && !code
        binding.codeLayout.isVisible = phone && code
        binding.login.setText(if (phone && !code) R.string.send_code else R.string.login)
        binding.toggleMethod.setText(if (phone) R.string.login_email else R.string.login_phone)
    }
    fun setBusy(value: Boolean) {
        if (!active) return
        busy = value
        binding.loading.isVisible = value
        listOf(binding.login, binding.google, binding.guest, binding.register, binding.toggleMethod).forEach { it.isEnabled = !value }
    }
    fun showError(id: Int) {
        if (active) { binding.error.setText(id); binding.error.isVisible = true }
    }
    private fun signIn() {
        if (busy) return
        if (!host.phoneSession.phoneMode) {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.isBlank()) { showError(R.string.invalid_login); return }
            setBusy(true)
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                setBusy(false)
                if (it.isSuccessful) host.rememberLoginMethod("Email") else showError(R.string.auth_failed)
            }
            return
        }
        val verification = host.phoneSession.verificationId
        if (verification.isEmpty()) {
            val phone = binding.phone.text.toString().trim()
            if (!phone.matches(Regex("\\+[1-9][0-9]{7,14}"))) { showError(R.string.invalid_login); return }
            setBusy(true)
            try {
                host.model.verifyPhoneNumber(phone, host, object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) { finishPhone(credential) }
                    override fun onVerificationFailed(error: FirebaseException) { setBusy(false); showError(R.string.auth_failed) }
                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        host.phoneSession.verificationId = id
                        if (active) { setBusy(false); updateMode() }
                    }
                })
            } catch (_: Exception) { setBusy(false); showError(R.string.auth_failed) }
        } else {
            val code = binding.code.text.toString().trim()
            if (!code.matches(Regex("[0-9]{6}"))) { showError(R.string.invalid_code); return }
            setBusy(true)
            finishPhone(PhoneAuthProvider.getCredential(verification, code))
        }
    }
    private fun finishPhone(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener {
            setBusy(false)
            if (it.isSuccessful) host.rememberLoginMethod("Phone") else showError(R.string.auth_failed)
        }
    }
}

class RegisterScreen(private val host: MainActivity,
    private val binding: ScreenRegisterBinding = ScreenRegisterBinding.inflate(host.layoutInflater)) : NativeScreen(binding.root) {
    init {
        binding.back.setOnClickListener { host.navigate(AppRoute.LOGIN) }
        binding.register.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString()
            val error = when {
                name.isBlank() || name.length > 80 || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 8 -> R.string.invalid_registration
                password != binding.confirm.text.toString() -> R.string.password_mismatch
                else -> null
            }
            if (error != null) { showError(error); return@setOnClickListener }
            binding.register.isEnabled = false
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { result ->
                if (active) binding.register.isEnabled = true
                if (!result.isSuccessful) showError(R.string.auth_failed) else {
                    val user = result.result.user
                    user?.updateProfile(userProfileChangeRequest { displayName = name })?.addOnCompleteListener { host.model.refreshProfile() }
                }
            }
        }
    }
    private fun showError(id: Int) { if (active) { binding.error.setText(id); binding.error.isVisible = true } }
}
