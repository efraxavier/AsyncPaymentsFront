package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityLoginBinding
import com.example.asyncpayments.model.AuthRequest
import com.example.asyncpayments.network.AuthService
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var loginAttempts = 0
    private val maxAttempts = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                ShowNotification.show(
                    this,
                    ShowNotification.Type.LOGIN_ERROR,
                    0.0,
                    "Preencha todos os campos"
                )
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        if (loginAttempts >= maxAttempts) {
            ShowNotification.show(
                this,
                ShowNotification.Type.LOGIN_ERROR,
                0.0,
                "Você excedeu o número máximo de tentativas. Tente novamente mais tarde."
            )
            return
        }

        val retrofit = RetrofitClient.getInstance(this)
        val authService = retrofit.create(AuthService::class.java)
        val authRequest = AuthRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = authService.login(authRequest)
                SharedPreferencesHelper(this@LoginActivity).saveToken(response.token)
                ShowNotification.show(
                    this@LoginActivity,
                    ShowNotification.Type.LOGIN_SUCCESS
                )
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            } catch (e: Exception) {
                loginAttempts++
                val msg = e.message ?: ""
                val userMsg = when {
                    msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ||
                    msg.contains("senha", ignoreCase = true) || msg.contains("password", ignoreCase = true) ->
                        "E-mail ou senha incorretos. Tentativas restantes: ${maxAttempts - loginAttempts}"
                    else -> "Erro ao fazer login: ${msg}"
                }
                ShowNotification.show(
                    this@LoginActivity,
                    ShowNotification.Type.LOGIN_ERROR,
                    0.0,
                    userMsg
                )
            }
        }
    }
}