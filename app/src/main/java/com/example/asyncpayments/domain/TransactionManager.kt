package com.example.asyncpayments.domain

import android.content.Context
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.network.TransactionService
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
        val queue = offlineQueue.loadAll(context)
        if (queue.isEmpty()) {
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
                        val request = TransactionRequest(
                            idUsuarioOrigem = idOrigem,
                            idUsuarioDestino = idDestino,
                            valor = transacao.valor,
                            metodoConexao = transacao.metodoConexao,
                            gatewayPagamento = transacao.gatewayPagamento,
                            descricao = transacao.descricao
                        )
                        transactionService.sendTransaction(request)
                        sucesso++
                    } else {
                        erro++
                    }
                } catch (e: Exception) {
                    erro++
                }
            }
            offlineQueue.clear(context)
            onResult(sucesso, erro)
        }
    }
}