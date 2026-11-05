package com.example.budgetwisesa.data.remote

import android.net.Uri
import com.example.budgetwisesa.data.model.Transaction
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CloudStore(private val uid: String) {
    private val project = requireNotNull(FirebaseApp.getInstance().options.projectId)
    private val api = Retrofit.Builder().baseUrl("https://firestore.googleapis.com/v1/")
        .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)

    private suspend fun authorization(): String {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        check(user?.uid == uid) { "Account changed" }
        val token = requireNotNull(user!!.getIdToken(false).await().token)
        check(auth.currentUser?.uid == uid) { "Account changed" }
        return "Bearer $token"
    }

    suspend fun put(collection: String, id: String, item: Any) {
        api.put(project, uid, collection, id, authorization(), FirestoreCodec.encode(item))
    }

    suspend fun <T> list(collection: String, type: Class<T>): List<T> {
        val result = mutableListOf<T>()
        var token: String? = null
        do {
            val page = api.list(project, uid, collection, authorization(), token)
            result += page.documents.orEmpty().map { FirestoreCodec.decode(it, type) }
            token = page.nextPageToken?.takeIf { it.isNotBlank() }
        } while (token != null)
        return result
    }

    suspend fun uploadReceipt(item: Transaction): Transaction {
        if (item.deleted) return item.copy(receiptPath = null)
        val path = item.receiptPath ?: return item
        if (path.startsWith("https://")) return item
        authorization() // Check the session before sending a private receipt.
        val reference = FirebaseStorage.getInstance().reference
            .child("users/$uid/receipts/${item.id}/${item.revision}")
        val contentType = when (path.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        reference.putFile(Uri.parse(path), StorageMetadata.Builder().setContentType(contentType).build()).await()
        return item.copy(receiptPath = reference.downloadUrl.await().toString())
    }
}
