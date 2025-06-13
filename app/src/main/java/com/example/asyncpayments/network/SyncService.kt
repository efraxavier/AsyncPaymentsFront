package com.example.asyncpayments.network

import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncService {
    @POST("sincronizacao/manual")
    suspend fun sincronizarTodas(): ResponseBody

    @POST("sincronizacao/me")
    suspend fun sincronizarMinhaConta(): ResponseBody

    @POST("sincronizacao/manual/{id}")
    suspend fun sincronizarManual(@Path("id") id: Long): ResponseBody
}