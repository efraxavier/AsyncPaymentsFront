package com.example.asyncpayments.domain

import android.content.Context
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.OfflineTransactionQueue

class TransactionUseCase(
    private val transactionService: TransactionService,
    private val offlineTransactionQueue: OfflineTransactionQueue
) {
    suspend fun sendTransactionOnline(request: TransactionRequest): TransactionResponse {
        return transactionService.sendTransaction(request)
    }

    fun saveTransactionOffline(
        context: Context,
        paymentData: PaymentData,
        usuarioOrigemId: Long,
        usuarioDestinoId: Long,
        saldoAtual: Double
    ) {
        offlineTransactionQueue.saveTransaction(context, paymentData, usuarioOrigemId, usuarioDestinoId, saldoAtual)
    }
}