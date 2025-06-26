package com.example.asyncpayments.domain

import com.example.asyncpayments.data.OfflineTransactionDao
import com.example.asyncpayments.data.OfflineTransactionEntity
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionSyncWorker(
    private val offlineTransactionDao: OfflineTransactionDao,
    private val api: TransactionService,
    private val usuarioAtualId: Long,
    private val context: android.content.Context,
    private val onRollback: (() -> Unit)? = null // <-- Adicione este callback
) {
    suspend fun atualizarStatusPendentes() {
        try {
            val pendentes: List<OfflineTransactionEntity> = offlineTransactionDao.getAll().filter { it.status == "PENDENTE" }
            for (transacao in pendentes) {
                val responses: List<TransactionResponse> = api.listarTransacoes(
                    tipoOperacao = null,
                    idUsuarioOrigem = null,
                    idUsuarioDestino = null,
                    status = null,
                    dataCriacaoInicio = null,
                    dataCriacaoFim = null
                ).filter { it.identificadorOffline == transacao.identificadorOffline }

                val minhaTransacao = responses.find {
                    it.idUsuarioOrigem == usuarioAtualId || it.idUsuarioDestino == usuarioAtualId
                }

                if (minhaTransacao != null && minhaTransacao.status != transacao.status) {
                    // Atualiza o status local
                    offlineTransactionDao.update(
                        transacao.copy(status = minhaTransacao.status)
                    )
                    AppLogger.log("TransactionSyncWorker", "Status atualizado para ${minhaTransacao.status} para transação ${transacao.identificadorOffline}")

                    // Se for rollback, reverte saldo local e notifica usuário
                    if (minhaTransacao.status == "ROLLBACK") {
                        reverterSaldoLocal(transacao)
                        notificarRollback(transacao)
                    } else if (minhaTransacao.status == "SINCRONIZADA") {
                        AppLogger.log("TransactionSyncWorker", "Transação sincronizada, saldo mantido para ${transacao.identificadorOffline}")
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.log("TransactionSyncWorker", "Erro ao sincronizar transações pendentes", e)
        }
    }

    private fun reverterSaldoLocal(transacao: OfflineTransactionEntity) {
        val prefs = SharedPreferencesHelper(context)
        val saldoAtual = prefs.getSaldoLocal() ?: 0.0
        val novoSaldo = if (transacao.idUsuarioOrigem == usuarioAtualId) {
            saldoAtual + transacao.valor
        } else {
            saldoAtual - transacao.valor
        }
        prefs.saveSaldoLocal(novoSaldo)
        onRollback?.invoke() // <-- Notifica a UI para atualizar
    }

    private fun notificarRollback(transacao: OfflineTransactionEntity) {
        AppLogger.log(
            "TransactionSyncWorker",
            "Usuário notificado sobre rollback da transação ${transacao.identificadorOffline} | Descrição: ${transacao.descricao}"
        )
        android.widget.Toast.makeText(
            context,
            "Transação revertida (rollback): ${transacao.descricao ?: ""}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun startPeriodicStatusSync(scope: CoroutineScope, intervalMillis: Long = 60_000L) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    atualizarStatusPendentes()
                } catch (e: Exception) {
                    AppLogger.log("TransactionSyncWorker", "Erro na sincronização periódica: ${e.message}")
                }
                delay(intervalMillis)
            }
        }
    }
}