package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityHomeBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.SyncService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    // Controle de visibilidade dos saldos
    private var syncVisible = true
    private var asyncVisible = true
    private var syncBalanceValue: Double = 0.0
    private var asyncBalanceValue: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val token = SharedPreferencesHelper(this).getToken()
        val email = token?.let { getEmailFromToken(it) } ?: ""
        val nomeUsuario = if (email.contains("@")) email.substringBefore("@") else email
        binding.tvWelcome.text = "Bem-vindo $nomeUsuario"
        setContentView(binding.root)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(R.id.menu_home).isChecked = true
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> true
                R.id.menu_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_transactions -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.btnSyncAccounts.setOnClickListener {
            val userId = getUserIdFromToken()
            if (userId != null) {
                sincronizarContas(userId)
            } else {
                showCustomDialog("Atenção", "Usuário não identificado")
            }
        }

        // Botão olho para saldo síncrono
        binding.btnToggleSyncBalance.setOnClickListener {
            syncVisible = !syncVisible
            updateSyncBalance()
        }

        // Botão olho para saldo assíncrono
        binding.btnToggleAsyncBalance.setOnClickListener {
            asyncVisible = !asyncVisible
            updateAsyncBalance()
        }

        carregarSaldosUsuario()
    }

    private fun updateSyncBalance() {
        if (syncVisible) {
            binding.tvSyncBalance.text = "R$ %.2f".format(syncBalanceValue)
            binding.btnToggleSyncBalance.setImageResource(R.drawable.ic_eye)
        } else {
            binding.tvSyncBalance.text = "••••••••"
            binding.btnToggleSyncBalance.setImageResource(R.drawable.ic_eye_off)
        }
    }

    private fun updateAsyncBalance() {
        if (asyncVisible) {
            binding.tvAsyncBalance.text = "R$ %.2f".format(asyncBalanceValue)
            binding.btnToggleAsyncBalance.setImageResource(R.drawable.ic_eye)
        } else {
            binding.tvAsyncBalance.text = "••••••••"
            binding.btnToggleAsyncBalance.setImageResource(R.drawable.ic_eye_off)
        }
    }

    private fun sincronizarContas(userId: Long) {
        val retrofit = RetrofitClient.getInstance(this)
        lifecycleScope.launch {
            try {
                val response = retrofit.create(SyncService::class.java).sincronizarManual(userId)
                val message = response.string()
                showCustomDialog("Sincronização", message)
                carregarSaldosUsuario()
            } catch (e: Exception) {
                showCustomDialog("Erro", "Erro ao sincronizar: ${e.message}")
            }
        }
    }

    private fun carregarSaldosUsuario() {
        val token = SharedPreferencesHelper(this).getToken() ?: return
        val emailLogado = getEmailFromToken(token) ?: return

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            try {
                val usuario = userService.getMe()
                if (usuario != null) {
                    syncBalanceValue = usuario.contaSincrona.saldo
                    asyncBalanceValue = usuario.contaAssincrona.saldo
                    updateSyncBalance()
                    updateAsyncBalance()
                } else {
                    binding.tvSyncBalance.text = "••••••••"
                    binding.tvAsyncBalance.text = "••••••••"
                }
            } catch (e: Exception) {
                showCustomDialog("Erro", "Erro ao carregar saldos: ${e.message}")
            }
        }
    }

    private fun getUserIdFromToken(): Long? {
        val token = SharedPreferencesHelper(this).getToken() ?: return null
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getLong("id")
        } catch (e: Exception) {
            null
        }
    }

    private fun getEmailFromToken(token: String): String? {
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getString("sub")
        } catch (e: Exception) {
            null
        }
    }

    private fun showCustomDialog(title: String, message: String) {
        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
            .show()
    }
}