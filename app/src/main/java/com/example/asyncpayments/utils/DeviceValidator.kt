package com.example.asyncpayments.utils

object DeviceValidator {
    private val trustedDevices = listOf("00:11:22:33:44:55", "66:77:88:99:AA:BB") 

    fun isDeviceTrusted(deviceAddress: String): Boolean {
        return trustedDevices.contains(deviceAddress)
    }
}