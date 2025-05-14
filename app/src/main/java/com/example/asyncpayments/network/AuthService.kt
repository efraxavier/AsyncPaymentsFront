package com.example.asyncpayments.network

import com.example.asyncpayments.model.AuthRequest
import com.example.asyncpayments.model.AuthResponse
import com.example.asyncpayments.model.RegisterRequest
import com.example.asyncpayments.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}