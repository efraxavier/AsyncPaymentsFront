package com.example.asyncpayments.network

import android.content.Context
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = SharedPreferencesHelper(context).getToken()
        AppLogger.log("AuthInterceptor", "Token usado na requisição: $token")
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}