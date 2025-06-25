package com.example.asyncpayments.comms

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.asyncpayments.model.PaymentData
import java.io.IOException
import java.io.ObjectOutputStream
import com.example.asyncpayments.utils.AppLogger
import java.util.UUID

class BluetoothSender(private val context: Context) {
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun send(data: PaymentData, device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppLogger.log("BluetoothSender", "Bluetooth connect permission not granted")
            return
        }
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(appUuid)
            socket.connect()
            val outputStream = ObjectOutputStream(socket.outputStream)
            outputStream.writeObject(data)
            outputStream.flush()
            AppLogger.log("BluetoothSender", "PaymentData sent successfully via Bluetooth")
        } catch (e: SecurityException) {
            AppLogger.log("BluetoothSender", "SecurityException sending Bluetooth: ${e.message}")
        } catch (e: IOException) {
            AppLogger.log("BluetoothSender", "Error sending PaymentData via Bluetooth: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (e: IOException) {
                AppLogger.log("BluetoothSender", "Error closing socket: ${e.message}")
            }
        }
    }
}