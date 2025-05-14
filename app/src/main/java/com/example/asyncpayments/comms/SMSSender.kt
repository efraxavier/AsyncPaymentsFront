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
            // Permission not granted, handle this case (e.g., show a message to the user)
            Log.e("SMSSender", "SMS permission not granted")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val message = "Payment Data: Amount=${data.amount}, Sender=${data.sender}, Receiver=${data.receiver}"
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SMSSender", "SMS sent successfully")
        } catch (e: SecurityException) {
            // Handle SecurityException here (permission denied, etc.)
            Log.e("SMSSender", "SecurityException sending SMS: ${e.message}")
        } catch (e: Exception){
            Log.e("SMSSender", "Generic exception sending SMS: ${e.message}")
        }
    }
}