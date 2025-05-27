package com.example.asyncpayments.comms

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

object NFCReceiver {
    fun extractPaymentData(intent: Intent): PaymentData? {
        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null && rawMessages.isNotEmpty()) {
            val ndefMessage = rawMessages[0] as NdefMessage
            val payload = ndefMessage.records[0].payload
            return try {
                val inputStream = ObjectInputStream(ByteArrayInputStream(payload))
                inputStream.readObject() as? PaymentData
            } catch (e: Exception) {
                Log.e("NFCReceiver", "Error reading PaymentData from NFC", e)
                null
            }
        }
        return null
    }
}