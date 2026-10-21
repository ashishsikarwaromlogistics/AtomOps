package com.example.atomops.utils


import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object JwtDecoder {

    fun decodePayload(token: String?): JSONObject? {
        return try {
            if (token.isNullOrBlank()) {
                return null
            }

            var jwtToken = token.trim()

            // Remove Bearer prefix if present
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7).trim()
            }

            val parts = jwtToken.split(".")

            if (parts.size != 3) {
                return null
            }

            // Decode the JWT payload (second part)
            val decodedBytes = Base64.decode(
                parts[1],
                Base64.URL_SAFE or
                        Base64.NO_WRAP or
                        Base64.NO_PADDING
            )

            val payload = String(
                decodedBytes,
                StandardCharsets.UTF_8
            )

            JSONObject(payload)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}