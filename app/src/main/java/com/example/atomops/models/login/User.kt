package com.example.atomops.models.login

data class User(
    val email: String,
    val firstName: String,
    val id: String,
    val lastLoginAt: String,
    val lastName: String,
    val permissions: List<String>,
    val roles: List<String>,
    val username: String
)