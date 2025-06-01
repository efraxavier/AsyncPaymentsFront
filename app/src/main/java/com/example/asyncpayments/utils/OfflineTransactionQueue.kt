package com.example.asyncpayments.utils

import android.content.Context
import com.example.asyncpayments.model.PaymentData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineTransactionQueue {
    private const val PREFS_NAME = "offline_queue"
    private const val KEY_QUEUE = "queue"

    fun save(context: Context, data: PaymentData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = loadAll(context).toMutableList()
        list.add(data)
        prefs.edit().putString(KEY_QUEUE, Gson().toJson(list)).apply()
    }

    fun loadAll(context: Context): List<PaymentData> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_QUEUE, "[]")
        val type = object : TypeToken<List<PaymentData>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}