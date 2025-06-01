package com.example.asyncpayments.utils

import android.content.Context
import com.example.asyncpayments.model.UserResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object UserCache {
    private const val PREFS_NAME = "user_cache"
    private const val KEY_USERS = "users"

    fun save(context: Context, users: List<UserResponse>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USERS, Gson().toJson(users)).apply()
    }

    fun load(context: Context): List<UserResponse> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USERS, "[]")
        val type = object : TypeToken<List<UserResponse>>() {}.type
        return Gson().fromJson(json, type)
    }
}