package com.example.asyncpayments.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SharedPreferencesHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("AsyncPaymentsPrefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "TOKEN"

    fun saveToken(token: String) {
        Log.d("SPHelper", "Salvando token: $token")
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String? {
        val token = prefs.getString(TOKEN_KEY, null)
        Log.d("SPHelper", "Lendo token: $token")
        return token
    }

    fun clearToken() {
        Log.d("SPHelper", "Limpando token")
        prefs.edit().remove(TOKEN_KEY).apply()
    }
}