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
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.TokenUtils
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64
import android.util.Log

class AddFundsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddFundsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFundsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddFunds.setOnClickListener {
            val valor = binding.etAddFundsValue.text.toString().toDoubleOrNull()
            val userId = TokenUtils.getUserIdFromToken(this)
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
        val service = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            try {
                AppLogger.log("AddFundsActivity", "Iniciando adição de fundos. userId=$userId, valor=$valor")
                val request = TransactionRequest(
                    idUsuarioOrigem = userId,
                    idUsuarioDestino = userId,
                    valor = valor,
                    tipoOperacao = "INTERNA",
                    metodoConexao = "ASYNC",
                    gatewayPagamento = "INTERNO",
                    descricao = "Adição de fundos à conta assincrona"
                )
                val apiResponse = service.sendTransaction(request)
                val mensagem = "Fundos adicionados com sucesso!\n" +
                               "Valor: R$ %.2f".format(apiResponse.valor)
                ShowNotification.show(
                    this@AddFundsActivity,
                    ShowNotification.Type.TRANSACTION_SENT,
                    apiResponse.valor,
                    mensagem
                )
            } catch (e: retrofit2.HttpException) {
                AppLogger.log("AddFundsActivity", "Erro ao adicionar fundos: ${e.response()?.errorBody()?.string()}", e)
                ShowNotification.show(
                    this@AddFundsActivity,
                    ShowNotification.Type.GENERIC,
                    valor,
                    "Erro ao adicionar fundos: ${e.response()?.errorBody()?.string()}"
                )
            } catch (e: Exception) {
                AppLogger.log("AddFundsActivity", "Erro ao adicionar fundos", e)
                ShowNotification.show(
                    this@AddFundsActivity,
                    ShowNotification.Type.GENERIC,
                    valor,
                    "Erro ao adicionar fundos: ${e.message}"
                )
            }
        }
    }
}