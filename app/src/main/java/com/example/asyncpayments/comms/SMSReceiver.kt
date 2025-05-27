package com.example.asyncpayments.comms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.example.asyncpayments.model.PaymentData

class SMSReceiver(private val onPaymentDataReceived: (PaymentData) -> Unit) : BroadcastReceiver() {
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
                            id = map["id"]?.toLongOrNull() ?: 0L,
                            valor = map["valor"]?.toDoubleOrNull() ?: 0.0,
                            origem = map["origem"] ?: "",
                            destino = map["destino"] ?: "",
                            data = map["data"] ?: ""
                        )
                        onPaymentDataReceived(paymentData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SMSReceiver", "Exception: ${e.message}")
        }
    }
}