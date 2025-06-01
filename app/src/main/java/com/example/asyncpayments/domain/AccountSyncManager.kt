package com.example.asyncpayments.domain

import com.example.asyncpayments.network.SyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AccountSyncManager(private val syncService: SyncService) {
    fun sincronizarMinhaConta(scope: CoroutineScope, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                val response = syncService.sincronizarMinhaConta()
                onSuccess(response.string())
            } catch (e: Exception) {
                onError(e.message ?: "Erro desconhecido")
            }
        }
    }
}