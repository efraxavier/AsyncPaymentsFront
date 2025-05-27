package com.example.asyncpayments.network

import com.example.asyncpayments.model.UserResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {
    @GET("usuarios/listar")
    suspend fun listarUsuarios(): List<UserResponse>
 
    @GET("usuarios/me")
    suspend fun getMe(): UserResponse

    @GET("usuarios/{id}")
    suspend fun getById(@Path("id") id: Long): UserResponse
}