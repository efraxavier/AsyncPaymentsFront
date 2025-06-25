package com.example.asyncpayments.utils

import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.TransactionService
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Date

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

    val sincronizacoes = transactionService.listarTransacoes(
        tipoOperacao = "SINCRONIZACAO",
        idUsuarioOrigem = userId,
        idUsuarioDestino = userId,
        status = null,
        dataCriacaoInicio = null,
        dataCriacaoFim = null
    )

    return (enviadas + recebidas + internas + sincronizacoes).distinctBy { it.id }
}

suspend fun sincronizarSeNecessario(
    transactionService: TransactionService,
    userId: Long,
    enviarSincronizacao: suspend () -> Unit
) {
    val sincronizacoes = transactionService.listarTransacoes(
        tipoOperacao = "SINCRONIZACAO",
        idUsuarioOrigem = userId,
        idUsuarioDestino = userId,
        status = "SINCRONIZADA"
    )

    val ultimaSincronizacao = sincronizacoes
        .filter { it.status == "SINCRONIZADA" && it.dataCriacao != null }
        .maxByOrNull {
            OffsetDateTime.parse(it.dataCriacao).toInstant().toEpochMilli()
        }

    val agora = OffsetDateTime.now().toInstant().toEpochMilli()
    val diffHoras = if (ultimaSincronizacao != null) {
        val ultima = OffsetDateTime.parse(ultimaSincronizacao.dataCriacao).toInstant().toEpochMilli()
        val diffMillis = agora - ultima
        diffMillis / (1000L * 60L * 60L)
    } else {
        9999L // Nunca sincronizou, força sincronizar
    }

    if (diffHoras >= 72) {
        enviarSincronizacao()
    }
}