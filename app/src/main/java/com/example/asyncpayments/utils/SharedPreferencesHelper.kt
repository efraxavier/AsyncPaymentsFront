package com.example.asyncpayments.utils

import android.content.Context
import com.example.asyncpayments.utils.AppLogger
import android.content.SharedPreferences
import android.util.Log

class SharedPreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("AsyncPaymentsPrefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "TOKEN"
    private val SALDO_KEY = "SALDO_LOCAL"

    fun saveToken(token: String) {
        AppLogger.log("SPHelper", "Salvando token: $token")
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        AppLogger.log("SPHelper", "Lendo token: $token")
        return token
    }

    fun clearToken() {
        AppLogger.log("SPHelper", "Limpando token")
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    fun saveSaldoLocal(saldo: Double) {
        AppLogger.log("SPHelper", "Salvando saldo local: $saldo")
        prefs.edit().putFloat(SALDO_KEY, saldo.toFloat()).apply()
    }

    fun getSaldoLocal(): Double? {
        val saldo = prefs.getFloat(SALDO_KEY, -1f)
        AppLogger.log("SPHelper", "Lendo saldo local: $saldo")
        return if (saldo >= 0f) saldo.toDouble() else null
    }

    fun clearSaldoLocal() {
        AppLogger.log("SPHelper", "Limpando saldo local")
        prefs.edit().remove(SALDO_KEY).apply()
    }
}