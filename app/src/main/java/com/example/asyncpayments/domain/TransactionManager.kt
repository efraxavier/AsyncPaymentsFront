package com.example.asyncpayments.domain

import android.content.Context
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineTransactionQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TransactionManager(
    private val transactionService: TransactionService,
    private val offlineQueue: OfflineTransactionQueue
) {
    fun sincronizarTransacoesOffline(
        context: Context,
        scope: CoroutineScope,
        buscarIdUsuarioPorEmail: suspend (String) -> Long?,
        onResult: (Int, Int) -> Unit
    ) {
        val queue: List<PaymentData> = offlineQueue.loadAll(context)
        if (queue.isEmpty()) {
            AppLogger.log("TransactionManager", "Nenhuma transação offline para sincronizar.")
            onResult(0, 0)
            return
        }
        scope.launch {
            var sucesso = 0
            var erro = 0
            for (transacao in queue) {
                try {
                    val idOrigem = buscarIdUsuarioPorEmail(transacao.origem)
                    val idDestino = buscarIdUsuarioPorEmail(transacao.destino)
                    if (idOrigem != null && idDestino != null) {
                        val tipoOperacao = if (transacao.metodoConexao == "INTERNET") "SINCRONA" else "ASSINCRONA"
                        val request = TransactionRequest(
                            idUsuarioOrigem = idOrigem,
                            idUsuarioDestino = idDestino,
                            valor = transacao.valor,
                            tipoOperacao = tipoOperacao,
                            metodoConexao = transacao.metodoConexao,
                            gatewayPagamento = transacao.gatewayPagamento,
                            descricao = transacao.descricao
                        )
                        transactionService.sendTransaction(request)
                        AppLogger.log("TransactionManager", "Transação sincronizada com sucesso: $request")
                        sucesso++
                    } else {
                        erro++
                    }
                } catch (e: Exception) {
                    AppLogger.log("TransactionManager", "Erro ao sincronizar transação: $transacao", e)
                    erro++
                }
            }
            offlineQueue.clear(context)
            AppLogger.log("TransactionManager", "Sincronização concluída. Sucesso: $sucesso, Erro: $erro")
            onResult(sucesso, erro)
        }
    }
}