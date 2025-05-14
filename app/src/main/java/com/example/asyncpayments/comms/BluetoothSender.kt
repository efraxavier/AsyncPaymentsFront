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
import java.util.UUID

class BluetoothSender(private val context: Context) {
    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun send(data: PaymentData, device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, handle this case (e.g., show a message to the user)
            Log.e("BluetoothSender", "Bluetooth connect permission not granted")
            return
        }
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(appUuid)
            socket.connect()

            val outputStream = ObjectOutputStream(socket.outputStream)
            outputStream.writeObject(data)
            outputStream.flush()
            Log.d("BluetoothSender", "Data sent successfully via Bluetooth")
        } catch (e: SecurityException) {
            // Handle SecurityException here (permission denied, etc.)
            Log.e("BluetoothSender", "SecurityException sending Bluetooth: ${e.message}")
        }catch (e: IOException) {
            Log.e("BluetoothSender", "Error sending data via Bluetooth: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (e: IOException) {
                Log.e("BluetoothSender", "Error closing socket: ${e.message}")
            }
        }
    }
}