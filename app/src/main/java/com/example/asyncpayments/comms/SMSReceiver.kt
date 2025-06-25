package com.example.asyncpayments.comms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineTransactionQueue

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle: Bundle? = intent.extras
        try {
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*>
                if (pdus != null) {
                    for (pdu in pdus) {
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                        val messageBody = sms.messageBody
                        val sender = sms.originatingAddress ?: "desconhecido"
                        AppLogger.log("SMSReceiver", "Mensagem recebida de $sender: $messageBody")
                        if (messageBody.startsWith("PAYMENTDATA;")) {
                            val fields = messageBody.removePrefix("PAYMENTDATA;").split(";")
                            val map = fields.mapNotNull {
                                val parts = it.split("=")
                                if (parts.size == 2) parts[0] to parts[1] else null
                            }.toMap()
                            val paymentData = PaymentData(
                                id = System.currentTimeMillis(),
                                valor = map["valor"]?.toDoubleOrNull() ?: 0.0,
                                origem = map["origem"] ?: "",
                                destino = map["destino"] ?: "",
                                data = System.currentTimeMillis().toString(),
                                metodoConexao = map["metodoConexao"] ?: "SMS",
                                gatewayPagamento = map["gatewayPagamento"] ?: "DREX",
                                descricao = map["descricao"] ?: "",
                                dataCriacao = System.currentTimeMillis() // <-- Adicione aqui
                            )
                            OfflineTransactionQueue.saveTransaction(context, paymentData)
                            AppLogger.log("SMSReceiver", "Transação salva offline via SMS: $paymentData")
                        }
                    }
                } else {
                    AppLogger.log("SMSReceiver", "Bundle pdus nulo")
                }
            }
        } catch (e: Exception) {
            AppLogger.log("SMSReceiver", "Exception: ${e.message}", e)
        }
    }
}