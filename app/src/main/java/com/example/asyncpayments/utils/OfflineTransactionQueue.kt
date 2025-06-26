package com.example.asyncpayments.utils

import android.content.Context
import com.example.asyncpayments.data.OfflineTransactionDao
import com.example.asyncpayments.data.OfflineTransactionEntity
import com.example.asyncpayments.model.PaymentData
import java.util.UUID
import kotlinx.coroutines.runBlocking

object OfflineTransactionQueue {
    private lateinit var dao: OfflineTransactionDao

    fun init(dao: OfflineTransactionDao) {
        this.dao = dao
        AppLogger.log("OfflineTransactionQueue", "DAO inicializado.")
    }

    fun saveTransaction(
        context: Context,
        paymentData: PaymentData,
        usuarioOrigemId: Long,
        usuarioDestinoId: Long,
        saldoAtual: Double
    ): Double {
        return runBlocking {
            AppLogger.log("OfflineTransactionQueue", "Iniciando registro offline: $paymentData")
            val identificadorOffline = paymentData.identificadorOffline.ifBlank { UUID.randomUUID().toString() }

            val entity = OfflineTransactionEntity(
                identificadorOffline = identificadorOffline,
                idUsuarioOrigem = usuarioOrigemId,
                idUsuarioDestino = usuarioDestinoId,
                valor = paymentData.valor,
                tipoOperacao = null,
                metodoConexao = paymentData.metodoConexao,
                gatewayPagamento = paymentData.gatewayPagamento,
                descricao = paymentData.descricao,
                status = "PENDENTE",
                dataCriacao = paymentData.dataCriacao
            )
            dao.insert(entity)
            AppLogger.log("OfflineTransactionQueue", "Transação salva offline (Room): $entity")
            // Antes de atualizar o saldo local
            AppLogger.log("OfflineTransactionQueue", "Saldo local antes do registro offline: $saldoAtual")

            // Corrigido: só subtrai se saldoAtual não for zero (ou negativo)
            val prefs = SharedPreferencesHelper(context)
            val saldoLocalAtual = prefs.getSaldoLocal()
            val saldoBase = if (saldoLocalAtual != null) saldoLocalAtual else saldoAtual

            val pendentes = dao.getAll().any { 
                it.status == "PENDENTE" && it.idUsuarioOrigem == usuarioOrigemId 
            }

            val novoSaldo = if (!pendentes) {
                // Primeira transação offline pendente: subtrai do saldo
                saldoBase - paymentData.valor
            } else {
                // Já existe pendente: mantém saldo local
                saldoBase
            }
            prefs.saveSaldoLocal(novoSaldo)
            AppLogger.log("OfflineTransactionQueue", "Saldo local atualizado para $novoSaldo após registro offline.")

            ShowNotification.show(
                context,
                ShowNotification.Type.GENERIC,
                novoSaldo,
                "Transação salva offline! Saldo atualizado localmente."
            )
            novoSaldo
        }
    }

    fun loadAll(context: Context): List<PaymentData> {
        return runBlocking {
            val agora = System.currentTimeMillis()
            dao.getAll().map {
                val expirou = (agora - it.dataCriacao) > 72 * 60 * 60 * 1000 // 72h em ms
                if (expirou && it.status == "PENDENTE") {
                    dao.update(it.copy(status = "ROLLBACK"))
                }
                PaymentData(
                    id = it.identificadorOffline.toLongOrNull() ?: System.currentTimeMillis(),
                    valor = it.valor,
                    origem = it.idUsuarioOrigem.toString(),
                    destino = it.idUsuarioDestino.toString(),
                    data = it.dataCriacao.toString(),
                    metodoConexao = it.metodoConexao,
                    gatewayPagamento = it.gatewayPagamento,
                    descricao = it.descricao,
                    dataCriacao = it.dataCriacao,
                    identificadorOffline = it.identificadorOffline
                )
            }.sortedByDescending { it.dataCriacao }
        }
    }

    fun clear(context: Context) {
        runBlocking {
            AppLogger.log("OfflineTransactionQueue", "Marcando todas as transações offline como sincronizadas.")
            dao.getAll().forEach { dao.update(it.copy(status = "SINCRONIZADA")) }
            AppLogger.log("OfflineTransactionQueue", "Fila de transações offline marcada como sincronizada (Room).")
        }
    }

    private fun usuarioAtualId(context: Context): Long {
        return TokenUtils.getUserIdFromToken(context) ?: 0L
    }
}