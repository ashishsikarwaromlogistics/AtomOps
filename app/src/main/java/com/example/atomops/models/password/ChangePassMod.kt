package com.example.atomops.models.password

data class ChangePassMod(
    val confirmPassword: String,
    val currentPassword: String,
    val newPassword: String
)