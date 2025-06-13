package com.example.asyncpayments.model

data class ApiResponse(
    val status: String,
    val message: String,
    val data: Any? 
)