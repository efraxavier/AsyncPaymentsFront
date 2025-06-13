package com.example.asyncpayments.utils

import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.TransactionService

suspend fun carregarTransacoesUsuario(
    transactionService: TransactionService,
    userId: Long,
    tipoConta: String?
): List<TransactionResponse> {
    val tipoOperacao = when (tipoConta) {
        "SINCRONA" -> "SINCRONA"
        "ASSINCRONA" -> "ASSINCRONA"
        else -> null
    }

    val enviadas = transactionService.listarTransacoes(
        tipoOperacao = tipoOperacao,
        idUsuarioOrigem = userId,
        idUsuarioDestino = null,
        status = null,
        dataCriacaoInicio = null,
        dataCriacaoFim = null
    )

    val recebidas = transactionService.listarTransacoes(
        tipoOperacao = tipoOperacao,
        idUsuarioOrigem = null,
        idUsuarioDestino = userId,
        status = null,
        dataCriacaoInicio = null,
        dataCriacaoFim = null
    )

    val internas = transactionService.listarTransacoes(
        tipoOperacao = "INTERNA",
        idUsuarioOrigem = userId,
        idUsuarioDestino = userId,
        status = null,
        dataCriacaoInicio = null,
        dataCriacaoFim = null
    )

    return (enviadas + recebidas + internas).distinctBy { it.id }
}