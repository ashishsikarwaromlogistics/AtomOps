package com.example.atomops.activities

import org.json.JSONObject
import retrofit2.Response

const val GENERIC_ERROR = "Something went wrong. Please try again."

/**
 * Returns the server's "detail" if present, otherwise a generic message.
 * errorBody() can be read only once, so call this once per response.
 */
fun <T> Response<T>.errorMessage(): String {
    return try {
        val raw = errorBody()?.string()
        if (raw.isNullOrBlank()) return GENERIC_ERROR
        val detail = JSONObject(raw).optString("detail", "")
        if (detail.isNotBlank()) detail else GENERIC_ERROR
    } catch (e: Exception) {
        GENERIC_ERROR
    }
}
