package com.example.asyncpayments.utils

import android.content.Context
import android.util.Log
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.model.PaymentData

object OfflineTransactionQueue {
    private val transactionQueue = mutableListOf<PaymentData>()

    fun saveTransaction(context: Context, paymentData: PaymentData) {
        try {
            transactionQueue.add(paymentData)
            AppLogger.log("OfflineTransactionQueue", "Transação salva offline: $paymentData")
        } catch (e: Exception) {
            AppLogger.log("OfflineTransactionQueue", "Erro ao salvar transação offline: ${e.message}", e)
        }
    }

    fun loadAll(context: Context): List<PaymentData> {
        AppLogger.log("OfflineTransactionQueue", "Carregando todas as transações offline. Total: ${transactionQueue.size}")
        return transactionQueue
    }

    fun clear(context: Context) {
        transactionQueue.clear()
        AppLogger.log("OfflineTransactionQueue", "Fila de transações offline limpa.")
    }
}