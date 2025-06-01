package com.example.asyncpayments.comms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.ShowNotification

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        try {
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<*>
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val messageBody = sms.messageBody
                    if (messageBody.startsWith("PAYMENTDATA;")) {
                        val fields = messageBody.removePrefix("PAYMENTDATA;").split(";")
                        val map = fields.mapNotNull {
                            val parts = it.split("=")
                            if (parts.size == 2) parts[0] to parts[1] else null
                        }.toMap()
                        val paymentData = PaymentData(
                            id = System.currentTimeMillis(),
                            valor = map["valor"]?.toDoubleOrNull() ?: 0.0,
                            origem = map["emailOrigem"] ?: "",
                            destino = map["emailDestino"] ?: "",
                            data = System.currentTimeMillis().toString(),
                            metodoConexao = map["metodoConexao"] ?: "INTERNET",
                            gatewayPagamento = map["gatewayPagamento"] ?: "INTERNO",
                            descricao = map["descricao"] ?: "" 
                        )
                        OfflineTransactionQueue.save(context, paymentData)

                        
                        ShowNotification.show(
                            context,
                            ShowNotification.Type.TRANSACTION_RECEIVED,
                            paymentData.valor,
                            paymentData.origem 
                        )

                        
                        Log.d("SMSReceiver", "Pagamento recebido: ${paymentData.valor} de ${paymentData.origem}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SMSReceiver", "Exception: ${e.message}")
        }
    }
}