package com.example.asyncpayments.utils

import android.content.Context
import android.util.Log
import com.example.asyncpayments.model.PaymentData

object OfflineTransactionQueue {
    private val transactionQueue = mutableListOf<PaymentData>()

    fun saveTransaction(context: Context, paymentData: PaymentData) {
        transactionQueue.add(paymentData)
        Log.d("OfflineTransactionQueue", "Transação salva offline: $paymentData")
    }

    fun loadAll(context: Context): List<PaymentData> {
        return transactionQueue
    }

    fun clear(context: Context) {
        transactionQueue.clear()
        Log.d("OfflineTransactionQueue", "Fila de transações offline limpa.")
    }
}