package com.example.atomops.models.apierror

import org.json.JSONObject
import retrofit2.Response

data class ApiError(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val errorCode: String? = null,
    val correlationId: String? = null
)

fun <T> Response<T>.errorMessage(): String {
    return try {
        val raw = errorBody()?.string()
        if (raw.isNullOrBlank()) {
            "Error ${code()}"
        } else {
            val json = JSONObject(raw)
            val detail = json.optString("detail", "")
            val title = json.optString("title", "")
            when {
                detail.isNotBlank() -> detail
                title.isNotBlank() -> title
                else -> "Error ${code()}"
            }
        }
    } catch (e: Exception) {
        "Error ${code()}"
    }
}