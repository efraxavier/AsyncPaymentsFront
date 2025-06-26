package com.example.asyncpayments.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_transactions")
data class OfflineTransactionEntity(
    @PrimaryKey val identificadorOffline: String,
    val idUsuarioOrigem: Long,
    val idUsuarioDestino: Long,
    val valor: Double,
    val tipoOperacao: String?,
    val metodoConexao: String,
    val gatewayPagamento: String,
    val descricao: String?,
    val status: String,
    val dataCriacao: Long
)