package com.example.asyncpayments.network

import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import retrofit2.http.*

interface TransactionService {
    @POST("transacoes")
    suspend fun sendTransaction(@Body request: TransactionRequest): TransactionResponse

    @GET("transacoes")
    suspend fun listarTransacoes(
        @Query("tipoOperacao") tipoOperacao: String? = null,
        @Query("idUsuarioOrigem") idUsuarioOrigem: Long? = null,
        @Query("idUsuarioDestino") idUsuarioDestino: Long? = null,
        @Query("status") status: String? = null,
        @Query("dataCriacaoInicio") dataCriacaoInicio: String? = null,
        @Query("dataCriacaoFim") dataCriacaoFim: String? = null
    ): List<TransactionResponse>

    @GET("transacoes/{id}")
    suspend fun buscarTransacaoPorId(@Path("id") id: Long): TransactionResponse

    @POST("transacoes/adicionar-fundos")
    suspend fun adicionarFundos(@Body request: TransactionRequest): TransactionResponse

    @GET("transacoes/recebidas")
    suspend fun listarTransacoesRecebidas(): List<TransactionResponse>

    @GET("transacoes/enviadas")
    suspend fun listarTransacoesEnviadas(): List<TransactionResponse>

    @GET("transacoes/{id}/status")
    suspend fun getTransactionStatus(@Path("id") id: Long): String
}