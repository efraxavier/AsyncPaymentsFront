package com.example.asyncpayments.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String = "USER"
)