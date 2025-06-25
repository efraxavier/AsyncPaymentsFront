package com.example.asyncpayments.utils

import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.model.UserResponse

data class UserSessionCache(
    val usuario: UserResponse?,
    val transacoesSync: List<TransactionResponse>,
    val transacoesAsync: List<TransactionResponse>
)