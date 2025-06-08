package com.example.asyncpayments.comms

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.utils.CryptoUtils
import java.io.ObjectOutputStream
import java.util.UUID

class SecureBluetoothSender {
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun send(data: PaymentData, device: BluetoothDevice) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(appUuid)
            socket.connect()
            val encryptedData = CryptoUtils.encrypt(data.toString().toByteArray())
            val outputStream = ObjectOutputStream(socket.outputStream)
            outputStream.write(encryptedData)
            outputStream.flush()
            Log.d("SecureBluetoothSender", "Encrypted PaymentData sent successfully via Bluetooth")
        } catch (e: Exception) {
            Log.e("SecureBluetoothSender", "Error sending encrypted PaymentData: ${e.message}")
        } finally {
            socket?.close()
        }
    }
}