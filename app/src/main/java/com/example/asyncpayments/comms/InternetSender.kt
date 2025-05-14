package com.example.asyncpayments.comms

import com.example.asyncpayments.model.PaymentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class InternetSender {

    private val client = OkHttpClient()

    suspend fun send(data: PaymentData, url: String) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("amount", data.amount)
                put("sender", data.sender)
                put("receiver", data.receiver)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to send data to server: ${response.code}")
                }
                // Handle successful response here (e.g., logging, callbacks)
                println("Data successfully sent to server.")
            }
        }
    }
}