package com.example.atomops.models.login

data class Data(
    val accessToken: String,
    val expiresIn: Int,
    val refreshToken: String,
    val tokenType: String,
    val user: User
)