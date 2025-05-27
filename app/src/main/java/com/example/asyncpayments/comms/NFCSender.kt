package com.example.asyncpayments.comms

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class NFCSender(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun enableForegroundDispatch() {
        if (nfcAdapter == null) return
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED))
        val techLists = arrayOf(arrayOf(Ndef::class.java.name))
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    fun disableForegroundDispatch() {
        if (nfcAdapter == null) return
        nfcAdapter.disableForegroundDispatch(activity)
    }

    fun preparePayload(data: PaymentData): NdefMessage {
        val outputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(outputStream)
        objectOutputStream.writeObject(data)
        objectOutputStream.flush()
        val payload = outputStream.toByteArray()

        val mimeType = "application/vnd.com.example.asyncpayments.paymentdata"
        val mimeRecord = NdefRecord.createMime(mimeType, payload)

        return NdefMessage(mimeRecord)
    }

    fun writeTag(tag: Tag?, ndefMessage: NdefMessage) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    // Tag too small
                } else {
                    ndef.writeNdefMessage(ndefMessage)
                    // Success
                }
                ndef.close()
            }
        } catch (e: Exception) {
            Log.e("NFCSender", "Error writing tag", e)
        }
    }
}