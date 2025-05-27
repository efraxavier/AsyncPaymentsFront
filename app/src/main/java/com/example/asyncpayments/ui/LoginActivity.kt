package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityLoginBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.model.AuthRequest
import com.example.asyncpayments.network.AuthService
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.utils.SharedPreferencesHelper
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var alertDialog: AlertDialog? = null

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
                showCustomDialog("Atenção", "Preencha todos os campos")
            }
        }

        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        val retrofit = RetrofitClient.getInstance(this)
        val authService = retrofit.create(AuthService::class.java)
        val authRequest = AuthRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = authService.login(authRequest)
                SharedPreferencesHelper(this@LoginActivity).saveToken(response.token)
                showCustomDialog("Login realizado", "Login realizado com sucesso!")
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            } catch (e: Exception) {
                showCustomDialog("Erro", "Erro no login: ${e.message}")
            }
        }
    }

    private fun showCustomDialog(title: String, message: String) {
        if (isFinishing || isDestroyed) return
        val dialogBinding = DialogResponseBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        alertDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        alertDialog?.show()
    }

    override fun onDestroy() {
        alertDialog?.dismiss()
        super.onDestroy()
    }
}