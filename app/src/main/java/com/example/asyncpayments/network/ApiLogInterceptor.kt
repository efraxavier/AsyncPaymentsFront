package com.example.asyncpayments.network

import com.example.asyncpayments.utils.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

class ApiLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val endpoint = request.url.encodedPath
        val method = request.method
        val requestBody = request.body?.toString() ?: ""
        AppLogger.log(
            "ApiLogInterceptor",
            "Request: [$method] $endpoint\nBody: $requestBody"
        )
        return try {
            val response = chain.proceed(request)
            val responseBody = response.peekBody(1024 * 1024).string()
            AppLogger.log(
                "ApiLogInterceptor",
                "Response: [$method] $endpoint\nCode: ${response.code}\nBody: $responseBody"
            )
            response
        } catch (e: Exception) {
            AppLogger.log(
                "ApiLogInterceptor",
                "Exception: [$method] $endpoint\n${e.message}",
                e
            )
            throw e
        }
    }
}