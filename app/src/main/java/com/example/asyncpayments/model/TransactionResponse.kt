package com.example.asyncpayments.model

data class TransactionResponse(
    val id: Long,
    val idUsuarioOrigem: Long,
    val idUsuarioDestino: Long,
    val valor: Double,
    val tipoTransacao: String?,
    val tipoOperacao: String?, 
    val metodoConexao: String,
    val gatewayPagamento: String,
    val descricao: String?, 
    val dataCriacao: String,
    val dataAtualizacao: String,
    val sincronizada: Boolean,
    val status: String, 
    val nomeUsuarioOrigem: String, 
    val emailUsuarioOrigem: String, 
    val cpfUsuarioOrigem: String, 
    val nomeUsuarioDestino: String, 
    val emailUsuarioDestino: String, 
    val cpfUsuarioDestino: String, 
    val dataSincronizacaoOrigem: String?, 
    val dataSincronizacaoDestino: String? 
)