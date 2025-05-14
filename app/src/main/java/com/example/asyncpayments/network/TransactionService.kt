package com.example.asyncpayments.network

import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TransactionService {
    @GET("transacoes/todas")
    suspend fun getAllTransactions(): List<TransactionResponse>

    @POST("transacoes/realizar")
    suspend fun sendTransaction(@Body request: TransactionRequest): TransactionResponse

    @POST("transacoes/adicionar-assincrona/{idUsuario}")
    suspend fun transferToAsyncAccount(@Path("idUsuario") userId: String): TransactionResponse
}