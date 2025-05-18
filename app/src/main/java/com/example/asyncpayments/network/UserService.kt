package com.example.asyncpayments.network

import com.example.asyncpayments.model.UserResponse
import retrofit2.http.GET

interface UserService {
    @GET("usuarios/listar")
    suspend fun listarUsuarios(): List<UserResponse>
}