package com.example.asyncpayments.network

import retrofit2.http.POST
import retrofit2.http.Path

data class SyncResponse(val message: String)

interface SyncService {
    @POST("sincronizacao/manual/{idUsuario}")
    suspend fun sincronizarManual(@Path("idUsuario") idUsuario: Long): SyncResponse
}