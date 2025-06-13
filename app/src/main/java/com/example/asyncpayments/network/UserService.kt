package com.example.asyncpayments.network

import com.example.asyncpayments.model.UserResponse
import retrofit2.http.*

interface UserService {
    @GET("usuarios")
    suspend fun listarUsuarios(): List<UserResponse>

    @GET("usuarios/me")
    suspend fun buscarMeuUsuario(): UserResponse

    @PUT("usuarios/me")
    suspend fun atualizarMeuUsuario(@Body user: UserResponse): UserResponse

    @DELETE("usuarios/me")
    suspend fun excluirMeuUsuario()

    @GET("usuarios/{id}")
    suspend fun buscarUsuarioPorId(@Path("id") id: Long): UserResponse

    @PUT("usuarios/{id}")
    suspend fun atualizarUsuario(@Path("id") id: Long, @Body user: UserResponse): UserResponse

    @DELETE("usuarios/{id}")
    suspend fun excluirUsuario(@Path("id") id: Long)
}