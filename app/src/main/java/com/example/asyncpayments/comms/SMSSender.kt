package com.example.asyncpayments.comms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.model.PaymentData

class SMSSender(private val context: Context) {
    fun send(data: PaymentData, phoneNumber: String) {
        AppLogger.log("SMSSender", "send() chamado. phoneNumber=$phoneNumber, data=$data")
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
            val message = "PAYMENTDATA;id=${data.id};valor=${data.valor};origem=${data.origem};destino=${data.destino};data=${data.data};metodoConexao=${data.metodoConexao};gatewayPagamento=${data.gatewayPagamento};descricao=${data.descricao}"
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            AppLogger.log("SMSSender", "SMS sent successfully para $phoneNumber")
        } catch (e: SecurityException) {
            AppLogger.log("SMSSender", "SecurityException sending SMS: ${e.message}")
        } catch (e: Exception){
            AppLogger.log("SMSSender", "Generic exception sending SMS: ${e.message}")
        }
    }
}