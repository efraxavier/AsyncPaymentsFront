package com.example.asyncpayments.model

data class TransactionRequest(
    val idUsuarioOrigem: Long,
    val idUsuarioDestino: Long,
    val valor: Double,
    val tipoOperacao: String,
    val metodoConexao: String,
    val gatewayPagamento: String,
    val descricao: String? = null 
)