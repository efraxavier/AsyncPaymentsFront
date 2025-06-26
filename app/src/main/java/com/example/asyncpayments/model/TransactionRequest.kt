package com.example.asyncpayments.model

import java.io.Serializable

data class TransactionRequest(
    val idUsuarioOrigem: Long,
    val idUsuarioDestino: Long,
    val valor: Double,
    val tipoOperacao: String?,
    val metodoConexao: String,
    val gatewayPagamento: String,
    val descricao: String?,
    val nomeUsuarioOrigem: String? = null,
    val emailUsuarioOrigem: String? = null,
    val cpfUsuarioOrigem: String? = null,
    val nomeUsuarioDestino: String? = null,
    val emailUsuarioDestino: String? = null,
    val cpfUsuarioDestino: String? = null
) : Serializable