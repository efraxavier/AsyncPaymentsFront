package com.example.asyncpayments.comms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.asyncpayments.model.PaymentData

class SMSSender(private val context: Context) {
    fun send(data: PaymentData, phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("SMSSender", "SMS permission not granted")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            
            val message = "PAYMENTDATA;id=${data.id};valor=${data.valor};origem=${data.origem};destino=${data.destino};data=${data.data}"
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SMSSender", "SMS sent successfully")
        } catch (e: SecurityException) {
            Log.e("SMSSender", "SecurityException sending SMS: ${e.message}")
        } catch (e: Exception){
            Log.e("SMSSender", "Generic exception sending SMS: ${e.message}")
        }
    }
}