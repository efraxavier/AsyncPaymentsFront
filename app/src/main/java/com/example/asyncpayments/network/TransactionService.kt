package com.example.asyncpayments.network

import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path

interface TransactionService {
    @POST("transacoes")
    suspend fun sendTransaction(@Body request: TransactionRequest): TransactionResponse

    @GET("transacoes")
    suspend fun getAllTransactions(): List<TransactionResponse>

    @GET("transacoes/{id}")
    suspend fun getTransactionById(@Path("id") id: Long): TransactionResponse

    @PUT("transacoes/{id}")
    suspend fun updateTransaction(@Path("id") id: Long, @Body request: TransactionRequest): TransactionResponse

    @DELETE("transacoes/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long)

    @GET("transacoes/recebidas")
    suspend fun getReceivedTransactions(): List<TransactionResponse>

}