package com.example.asyncpayments.comms

import com.example.asyncpayments.model.PaymentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class InternetReceiver {

    private val client = OkHttpClient()

    suspend fun receive(url: String): List<PaymentData> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to fetch data from server: ${response.code}")
            }
            val body = response.body?.string() ?: return@use emptyList<PaymentData>()
            val jsonArray = JSONArray(body)
            val result = mutableListOf<PaymentData>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    PaymentData(
                        id = obj.getLong("id"),
                        valor = obj.getDouble("valor"),
                        origem = obj.getString("origem"),
                        destino = obj.getString("destino"),
                        data = obj.getString("data")
                    )
                )
            }
            result
        }
    }
}