package com.example.asyncpayments.domain

import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AccountManager(
    private val userService: UserService,
    private val prefs: SharedPreferencesHelper
) {
    fun carregarSaldos(scope: CoroutineScope, onResult: (Double, Double) -> Unit, onError: (String) -> Unit) {
        val token = prefs.getToken() ?: return
        scope.launch {
            try {
                val usuario = userService.getMe()
                val sync = usuario?.contaSincrona?.saldo ?: 0.0
                val async = usuario?.contaAssincrona?.saldo ?: 0.0
                onResult(sync, async)
            } catch (e: Exception) {
                onError(e.message ?: "Erro desconhecido")
            }
        }
    }
}