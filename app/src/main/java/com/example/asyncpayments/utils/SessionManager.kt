package com.example.asyncpayments.utils

import android.content.Context

object SessionManager {
    private var token: String? = null

    fun getToken(context: Context): String? {
        if (token == null) {
            token = SharedPreferencesHelper(context).getToken()
        }
        return token
    }

    fun setToken(context: Context, value: String) {
        token = value
        SharedPreferencesHelper(context).saveToken(value)
    }

    fun clearToken(context: Context) {
        token = null
        SharedPreferencesHelper(context).clearToken()
    }
}