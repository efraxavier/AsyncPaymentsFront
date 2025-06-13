package com.example.asyncpayments.network

import com.example.asyncpayments.model.AuthRequest
import com.example.asyncpayments.model.AuthResponse
import com.example.asyncpayments.model.RegisterRequest
import com.example.asyncpayments.model.RegisterResponse
import retrofit2.http.*

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun registrar(@Body request: RegisterRequest): RegisterResponse

    @GET("auth/me/id")
    suspend fun buscarMeuId(): Long

    @GET("auth/user/id")
    suspend fun buscarIdPorEmail(@Query("email") email: String): Long

    @GET("auth/test")
    suspend fun testarAutenticacao(): String
}