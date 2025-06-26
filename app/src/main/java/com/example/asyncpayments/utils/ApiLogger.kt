package com.example.asyncpayments.utils

object ApiLogger {
    fun logApiCall(
        endpoint: String,
        operacao: String,
        requestBody: Any? = null,
        responseBody: Any? = null,
        exception: Throwable? = null
    ) {
        val sb = StringBuilder()
        sb.append("API CALL\n")
        sb.append("Endpoint: $endpoint\n")
        sb.append("Operação: $operacao\n")
        if (requestBody != null) sb.append("Request: $requestBody\n")
        if (responseBody != null) sb.append("Response: $responseBody\n")
        if (exception != null) sb.append("Exception: ${exception.message}\n")
        AppLogger.log("ApiLogger", sb.toString(), exception)
    }
}