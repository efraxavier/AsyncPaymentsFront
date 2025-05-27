package com.example.asyncpayments.comms

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.nfc.Tag
import com.example.asyncpayments.model.PaymentData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ConnectionType { BLUETOOTH, NFC, SMS, INTERNET }

class PaymentDispatcher(
    private val context: Context,
    private val activity: Activity,
    private val bluetoothDevice: BluetoothDevice?,
    private val nfcTag: Tag?,
    private val smsPhone: String?,
    private val serverUrl: String
) {
    fun dispatch(data: PaymentData, types: List<ConnectionType>) {
        types.forEach { type ->
            when (type) {
                ConnectionType.BLUETOOTH -> bluetoothDevice?.let {
                    BluetoothSender(context).send(data, it)
                }
                ConnectionType.NFC -> nfcTag?.let {
                    val nfcSender = NFCSender(activity)
                    val message = nfcSender.preparePayload(data)
                    nfcSender.writeTag(it, message)
                }
                ConnectionType.SMS -> smsPhone?.let {
                    SMSSender(context).send(data, it)
                }
                ConnectionType.INTERNET -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        InternetSender().send(data, serverUrl)
                    }
                }
            }
        }
    }
}