package com.example.asyncpayments.ui
import com.example.asyncpayments.model.TransactionRequest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityAddFundsBinding
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64

class AddFundsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddFundsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFundsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddFunds.setOnClickListener {
            val valor = binding.etAddFundsValue.text.toString().toDoubleOrNull()
            val userId = getUserIdFromToken()
            if (valor != null && userId != null) {
                addFunds(userId, valor)
            } else {
                ShowNotification.show(
                    this,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Preencha o valor corretamente"
                )
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.example.asyncpayments.R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_add_funds -> true
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
    }

    private fun addFunds(userId: Long, valor: Double) {
        val descricao = "" 
        val service = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            try {
                val request = TransactionRequest(
                    idUsuarioOrigem = userId,
                    idUsuarioDestino = userId,
                    valor = valor,
                    metodoConexao = "ASYNC",
                    gatewayPagamento = "INTERNO",
                    descricao = descricao
                )
                val apiResponse = service.sendTransaction(request)
                val mensagem = "Método: ${apiResponse.metodoConexao}\n" +
                               "Enviado para ID: ${apiResponse.idUsuarioDestino}\n" +
                               "Valor: R$ %.2f".format(apiResponse.valor)
                ShowNotification.show(
                    this@AddFundsActivity,
                    ShowNotification.Type.TRANSACTION_SENT,
                    apiResponse.valor,
                    mensagem
                )
            } catch (e: Exception) {
                ShowNotification.show(
                    this@AddFundsActivity,
                    ShowNotification.Type.GENERIC,
                    valor,
                    "Erro ao adicionar fundos: ${e.message}"
                )
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
}