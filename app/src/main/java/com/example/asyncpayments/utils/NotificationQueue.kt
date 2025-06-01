package com.example.asyncpayments.utils

import com.example.asyncpayments.model.TransactionResponse

object NotificationQueue {
    private val receivedTransactions = mutableListOf<TransactionResponse>()

    fun add(transaction: TransactionResponse) {
        receivedTransactions.add(transaction)
    }

    fun getAndClearAll(): List<TransactionResponse> {
        val list = receivedTransactions.toList()
        receivedTransactions.clear()
        return list
    }
}