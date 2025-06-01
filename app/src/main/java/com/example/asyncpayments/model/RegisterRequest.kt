package com.example.asyncpayments.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val cpf: String,
    val nome: String,
    val sobrenome: String,
    val celular: String,
    val role: String = "USER",
    val consentimentoDados: Boolean = true
)