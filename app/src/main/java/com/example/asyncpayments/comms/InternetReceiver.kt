package com.example.asyncpayments.comms

import android.content.Context
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.utils.ShowNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class InternetReceiver {

    private val client = OkHttpClient()

    suspend fun receive(context: Context, url: String): List<PaymentData> = withContext(Dispatchers.IO) {
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
                val paymentData = PaymentData(
                    id = System.currentTimeMillis(),
                    valor = obj.getDouble("valor"),
                    origem = obj.getString("origem"),
                    destino = obj.getString("destino"),
                    data = System.currentTimeMillis().toString(),
                    metodoConexao = obj.optString("metodoConexao", "INTERNET"),
                    gatewayPagamento = obj.optString("gatewayPagamento", "INTERNO"),
                    descricao = obj.getString("descricao") 
                )
                result.add(paymentData)
            }

            for (paymentData in result) {
                ShowNotification.show(
                    context,
                    ShowNotification.Type.TRANSACTION_RECEIVED,
                    paymentData.valor,
                    paymentData.origem
                )
            }
            result
        }
    }
}