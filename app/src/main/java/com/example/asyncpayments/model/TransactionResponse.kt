package com.example.asyncpayments.model

data class TransactionResponse(
    val id: Long,
    val idUsuarioOrigem: Long,
    val idUsuarioDestino: Long,
    val valor: Double,
    val tipoTransacao: String,
    val metodoConexao: String,
    val gatewayPagamento: String,
    val dataCriacao: String,
    val dataAtualizacao: String,
    val sincronizada: Boolean
)