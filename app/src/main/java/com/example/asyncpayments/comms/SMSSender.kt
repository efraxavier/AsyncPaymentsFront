package com.example.asyncpayments.comms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.model.PaymentData

class SMSSender(private val context: Context) {

    private fun getEmulatorNumber(): String? {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val lineNumber = telephonyManager.line1Number
        // Para emulador, geralmente retorna "+1555521XXXX"
        return lineNumber?.takeLast(4)
    }

    private fun getOtherEmulatorNumber(): String? {
        val myNumber = getEmulatorNumber()
        // Suporte para dois emuladores: 5554 <-> 5556
        return when (myNumber) {
            "5554" -> "5556"
            "5556" -> "5554"
            else -> null
        }
    }

    fun send(data: PaymentData, phoneNumber: String? = null) {
        val destino = phoneNumber ?: getOtherEmulatorNumber()
        AppLogger.log("SMSSender", "Preparando para enviar SMS. phoneNumber=$destino, data=$data")
        if (destino == null) {
            AppLogger.log("SMSSender", "Não foi possível determinar o número do outro emulador.")
            return
        }
        AppLogger.log("SMSSender", "send() chamado. phoneNumber=$destino, data=$data")
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppLogger.log("SMSSender", "SMS permission not granted")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val message = "PAYMENTDATA;id=${data.id};valor=${data.valor};origem=${data.origem};destino=${data.destino};data=${data.data};metodoConexao=${data.metodoConexao};gatewayPagamento=${data.gatewayPagamento};descricao=${data.descricao};identificadorOffline=${data.identificadorOffline}"
            AppLogger.log("SMSSender", "Enviando mensagem SMS: $message")
            smsManager.sendTextMessage(destino, null, message, null, null)
            AppLogger.log("SMSSender", "SMS sent successfully para $destino")
        } catch (e: SecurityException) {
            AppLogger.log("SMSSender", "SecurityException sending SMS: ${e.message}")
        } catch (e: Exception){
            AppLogger.log("SMSSender", "Generic exception sending SMS: ${e.message}")
        }
    }
}