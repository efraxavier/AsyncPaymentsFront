package com.example.asyncpayments.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

object AppLogger {
    private val logs = CopyOnWriteArrayList<String>()

    // Configurações do backend
    private const val LOGIN_URL = "http://10.0.2.2:8080/auth/login"
    private const val LOGS_URL = "http://10.0.2.2:8080/api/logs"
    private const val USERNAME = "admin"
    private const val PASSWORD = "123456"

    private var token: String? = null
    private val client = OkHttpClient()

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        val logMsg = "${System.currentTimeMillis()} [$tag] $message"
        logs.add(logMsg)
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable)
        } else {
            android.util.Log.d(tag, message)
        }
        sendLogsIfPossible()
    }

    fun getLogs(): String = logs.joinToString("\n")

    fun clear() = logs.clear()

    private fun sendLogsIfPossible() {
        if (token == null) {
            // Faz login e depois envia os logs
            loginAndSendLogs()
        } else {
            // Já tem token, envia direto
            sendLogsToBackend(LOGS_URL, token)
        }
    }

    private fun loginAndSendLogs() {
        val json = JSONObject()
            .put("email", USERNAME)
            .put("password", PASSWORD)
            .toString()
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("AppLogger", "Falha ao autenticar: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val respBody = response.body?.string()
                    val respJson = JSONObject(respBody)
                    token = respJson.optString("token")
                    sendLogsToBackend(LOGS_URL, token)
                } else {
                    android.util.Log.e("AppLogger", "Falha ao autenticar: ${response.code}")
                }
            }
        })
    }

    private fun sendLogsToBackend(endpoint: String, token: String?) {
        val logsToSend = getLogs()
        if (logsToSend.isBlank()) return
        val body = FormBody.Builder()
            .add("logs", logsToSend)
            .build()
        val builder = Request.Builder()
            .url(endpoint)
            .post(body)
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        val request = builder.build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("AppLogger", "Falha ao enviar logs: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                android.util.Log.d("AppLogger", "Logs enviados, status: ${response.code}")
                if (response.isSuccessful) clear()
            }
        })
    }
}