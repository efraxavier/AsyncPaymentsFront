package com.example.asyncpayments

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityLoginBinding
import com.example.asyncpayments.model.AuthRequest
import com.example.asyncpayments.network.AuthService
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.ui.RegisterActivity
import com.example.asyncpayments.ui.TransactionActivity
import com.example.asyncpayments.utils.SharedPreferencesHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

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
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
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
                SharedPreferencesHelper(this@MainActivity).saveToken(response.token)
                startActivity(Intent(this@MainActivity, TransactionActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro no login: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}