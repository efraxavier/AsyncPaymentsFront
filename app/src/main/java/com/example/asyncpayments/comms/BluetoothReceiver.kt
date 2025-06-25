package com.example.asyncpayments.comms

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.example.asyncpayments.utils.AppLogger
import android.util.Log
import com.example.asyncpayments.model.PaymentData
import java.io.IOException
import java.io.ObjectInputStream
import java.util.UUID

class BluetoothReceiver {
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun listenForData(onDataReceived: (PaymentData) -> Unit) {
        val serverSocket: BluetoothServerSocket? =
            BluetoothAdapter.getDefaultAdapter()
                ?.listenUsingRfcommWithServiceRecord("AsyncPayments", appUuid)
        var socket: BluetoothSocket? = null
        try {
            socket = serverSocket?.accept()
            val inputStream = ObjectInputStream(socket?.inputStream)
            val data = inputStream.readObject() as? PaymentData
            data?.let { onDataReceived(it) }
            AppLogger.log("BluetoothReceiver", "PaymentData received via Bluetooth")
        } catch (e: IOException) {
            AppLogger.log("BluetoothReceiver", "Error receiving PaymentData: ${e.message}")
        } catch (e: ClassNotFoundException) {
            AppLogger.log("BluetoothReceiver", "Class not found: ${e.message}")
        } finally {
            try {
                socket?.close()
                serverSocket?.close()
            } catch (e: IOException) {
                AppLogger.log("BluetoothReceiver", "Error closing socket: ${e.message}")
            }
        }
    }
}