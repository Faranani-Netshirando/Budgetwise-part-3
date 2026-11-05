package com.example.budgetwisesa.data.remote

import retrofit2.http.*

/** Firestore's database-backed REST API, authenticated with a Firebase user ID token.
 * Reference: https://firebase.google.com/docs/firestore/use-rest-api
 */
interface ApiService {
    @GET("projects/{project}/databases/(default)/documents/users/{uid}/{collection}")
    suspend fun list(
        @Path("project") project: String, @Path("uid") uid: String,
        @Path("collection") collection: String, @Header("Authorization") authorization: String,
        @Query("pageToken") pageToken: String? = null, @Query("pageSize") pageSize: Int = 100
    ): DocumentPage

    @PATCH("projects/{project}/databases/(default)/documents/users/{uid}/{collection}/{id}")
    suspend fun put(
        @Path("project") project: String, @Path("uid") uid: String,
        @Path("collection") collection: String, @Path("id") id: String,
        @Header("Authorization") authorization: String, @Body document: CloudDocument
    ): CloudDocument
}

data class DocumentPage(val documents: List<CloudDocument>? = null, val nextPageToken: String? = null)
data class CloudDocument(val name: String? = null, val fields: Map<String, CloudValue> = emptyMap())
data class CloudValue(
    val stringValue: String? = null, val integerValue: String? = null,
    val doubleValue: Double? = null, val booleanValue: Boolean? = null
)
