package com.example.budgetwisesa.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object FirestoreCodec {
    private val gson = Gson()

    fun encode(value: Any): CloudDocument {
        val fields = gson.toJsonTree(value).asJsonObject.entrySet()
            .filter { it.key != "isSynced" && it.value.isJsonPrimitive }
            .associate { (key, element) ->
                val p = element.asJsonPrimitive
                key to when {
                    p.isBoolean -> CloudValue(booleanValue = p.asBoolean)
                    p.isNumber && p.asString.matches(Regex("-?[0-9]+")) -> CloudValue(integerValue = p.asString)
                    p.isNumber -> CloudValue(doubleValue = p.asDouble)
                    else -> CloudValue(stringValue = p.asString)
                }
            }
        return CloudDocument(fields = fields)
    }

    fun <T> decode(document: CloudDocument, type: Class<T>): T {
        val json = JsonObject()
        document.fields.forEach { (key, value) ->
            val primitive = when {
                value.stringValue != null -> JsonPrimitive(value.stringValue)
                value.integerValue != null -> JsonPrimitive(value.integerValue.toLong())
                value.doubleValue != null -> JsonPrimitive(value.doubleValue)
                value.booleanValue != null -> JsonPrimitive(value.booleanValue)
                else -> null
            }
            if (primitive != null) json.add(key, primitive)
        }
        require(json.get("id")?.asString == document.name?.substringAfterLast('/')) { "Invalid cloud record ID" }
        // Prototype documents used numeric IDs and had no sync metadata. Preserve surviving records.
        json.addProperty("id", document.name!!.substringAfterLast('/'))
        if (!json.has("revision")) json.addProperty("revision", "legacy-" + json.get("id").asString)
        if (!json.has("deleted")) json.addProperty("deleted", false)
        json.addProperty("isSynced", true)
        return gson.fromJson(json, type)
    }
}
