package com.example.asyncpayments.model

import java.io.Serializable

data class PaymentData(
    val amount: Double,
    val sender: String,
    val receiver: String
) : Serializable