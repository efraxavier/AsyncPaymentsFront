package com.example.asyncpayments.utils

import android.content.Context
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.model.UserResponse
import com.google.gson.Gson

object SessionCacheUtils {
    private const val PREFS_NAME = "session_cache"
    private const val KEY = "user_session"

    fun saveSessionCache(
        context: Context,
        usuario: UserResponse?,
        transacoesSync: List<TransactionResponse>,
        transacoesAsync: List<TransactionResponse>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val cache = UserSessionCache(usuario, transacoesSync, transacoesAsync)
        prefs.edit().putString(KEY, gson.toJson(cache)).apply()
    }

    fun loadSessionCache(context: Context): UserSessionCache? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null)
        return if (json != null) Gson().fromJson(json, UserSessionCache::class.java) else null
    }
}