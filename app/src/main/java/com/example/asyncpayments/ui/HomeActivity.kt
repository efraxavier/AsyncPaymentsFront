package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityHomeBinding
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.SyncService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(com.example.asyncpayments.R.id.menu_home).isChecked = true
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.example.asyncpayments.R.id.menu_home -> true
                com.example.asyncpayments.R.id.menu_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_transactions -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_profile -> {
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
                Toast.makeText(this, "Usuário não identificado", Toast.LENGTH_SHORT).show()
            }
        }

        carregarSaldosUsuario()
    }

    private fun sincronizarContas(userId: Long) {
        val retrofit = RetrofitClient.getInstance(this)
        lifecycleScope.launch {
            try {
                val response = retrofit.create(SyncService::class.java)
                    .sincronizarManual(userId)
                Toast.makeText(this@HomeActivity, response.message, Toast.LENGTH_LONG).show()
                carregarSaldosUsuario()
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Erro ao sincronizar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun carregarSaldosUsuario() {
        val token = SharedPreferencesHelper(this).getToken() ?: return
        val emailLogado = getEmailFromToken(token) ?: return

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            try {
                val usuarios = userService.listarUsuarios()
                val usuario = usuarios.find { it.email == emailLogado }
                if (usuario != null) {
                    binding.tvSyncBalance.text = "Saldo Síncrono: R$ %.2f".format(usuario.contaSincrona.saldo)
                    binding.tvAsyncBalance.text = "Saldo Assíncrono: R$ %.2f".format(usuario.contaAssincrona.saldo)
                } else {
                    binding.tvSyncBalance.text = "Saldo Síncrono: --"
                    binding.tvAsyncBalance.text = "Saldo Assíncrono: --"
                }
            } catch (e: Exception) {
                Toast.makeText(this@HomeActivity, "Erro ao carregar saldos: ${e.message}", Toast.LENGTH_SHORT).show()
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
}