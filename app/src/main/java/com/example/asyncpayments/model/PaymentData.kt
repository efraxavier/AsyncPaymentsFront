package com.example.asyncpayments.model

import java.io.Serializable

data class PaymentData(
    val id: Long,
    val valor: Double,
    val origem: String,
    val destino: String,
    val data: String,
    val metodoConexao: String,
    val gatewayPagamento: String,
    val descricao: String? = null,
    val dataCriacao: Long
) : Serializable