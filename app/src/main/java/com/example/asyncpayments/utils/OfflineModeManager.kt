package com.example.asyncpayments.utils

object OfflineModeManager {
    @Volatile
    var isOffline: Boolean = false
}