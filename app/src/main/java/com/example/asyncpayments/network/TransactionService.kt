package com.example.asyncpayments.network

import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionService {
    @POST("transacoes/adicionar-assincrona/{idUsuario}")
    suspend fun addFundsAsync(
        @Path("idUsuario") userId: Long,
        @Query("idUsuarioDestino") idUsuarioDestino: Long,
        @Query("valor") valor: Double,
        @Body body: Map<String, Double>
    ): TransactionResponse

    @POST("transacoes/realizar")
    suspend fun sendTransaction(@Body request: TransactionRequest): TransactionResponse

    @GET("transacoes/todas")
    suspend fun getAllTransactions(
        @Query("idUsuarioDestino") idUsuarioDestino: Long? = null,
        @Query("valor") valor: Double? = null
    ): List<TransactionResponse>
}