package com.example.budgetwisesa.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.security.MessageDigest

object PushRegistration {
    private fun key(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    fun register() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
                FirebaseFirestore.getInstance().collection("users").document(uid).collection("devices")
                    .document(key(token)).set(mapOf("token" to token, "platform" to "android"))
            }
        }
    }
    fun unregister() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (FirebaseAuth.getInstance().currentUser?.uid == uid) {
                FirebaseFirestore.getInstance().collection("users").document(uid).collection("devices").document(key(token)).delete()
            }
        }
    }
}
