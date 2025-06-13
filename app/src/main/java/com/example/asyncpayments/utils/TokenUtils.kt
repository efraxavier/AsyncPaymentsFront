package com.example.asyncpayments.utils

import android.content.Context
import android.util.Base64
import org.json.JSONObject

object TokenUtils {
    fun getUserIdFromToken(context: Context): Long? {
        val token = SharedPreferencesHelper(context).getToken() ?: return null
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getLong("id")
        } catch (e: Exception) {
            null
        }
    }

    fun getEmailFromToken(context: Context): String? {
        val token = SharedPreferencesHelper(context).getToken() ?: return null
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getString("sub")
        } catch (e: Exception) {
            null
        }
    }
}