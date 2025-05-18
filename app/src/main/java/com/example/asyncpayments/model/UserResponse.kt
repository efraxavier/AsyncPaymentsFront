package com.example.asyncpayments.model

data class UserResponse(
    val id: Long,
    val email: String,
    val role: String,
    val contaSincrona: ContaSincrona,
    val contaAssincrona: ContaAssincrona
)

data class ContaSincrona(
    val id: Long,
    val saldo: Double,
    val tipoConta: String
)

data class ContaAssincrona(
    val id: Long,
    val saldo: Double,
    val bloqueada: Boolean,
    val ultimaSincronizacao: String,
    val tipoConta: String
)