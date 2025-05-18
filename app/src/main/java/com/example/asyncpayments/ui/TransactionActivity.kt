package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityTransactionBinding
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import kotlinx.coroutines.launch

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendTransaction.setOnClickListener {
            val valor = binding.etAmount.text.toString().toDoubleOrNull()
            val idUsuarioOrigem = binding.etIdUsuarioOrigem.text.toString().toLongOrNull()
            val idUsuarioDestino = binding.etIdUsuarioDestino.text.toString().toLongOrNull()
            val metodoConexao = binding.etMetodoConexao.text.toString().ifEmpty { "INTERNET" }
            val gatewayPagamento = binding.etGatewayPagamento.text.toString().ifEmpty { "STRIPE" }

            if (valor != null && idUsuarioOrigem != null && idUsuarioDestino != null) {
                sendTransaction(idUsuarioOrigem, idUsuarioDestino, valor, metodoConexao, gatewayPagamento)
            } else {
                Toast.makeText(this, "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                com.example.asyncpayments.R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_transactions -> true
                com.example.asyncpayments.R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun sendTransaction(
        idUsuarioOrigem: Long,
        idUsuarioDestino: Long,
        valor: Double,
        metodoConexao: String,
        gatewayPagamento: String
    ) {
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        val request = TransactionRequest(
            idUsuarioOrigem = idUsuarioOrigem,
            idUsuarioDestino = idUsuarioDestino,
            valor = valor,
            metodoConexao = metodoConexao,
            gatewayPagamento = gatewayPagamento
        )

        lifecycleScope.launch {
            try {
                val apiResponse = transactionService.sendTransaction(request)
                Toast.makeText(this@TransactionActivity, apiResponse.toString(), Toast.LENGTH_LONG).show()
                mostrarUltimaTransacao(idUsuarioDestino)
            } catch (e: Exception) {
                Toast.makeText(this@TransactionActivity, "Erro ao realizar transação: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarUltimaTransacao(idUsuarioDestino: Long) {
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            try {
                val transacoes = transactionService.getAllTransactions(idUsuarioDestino)
                val ultima = transacoes.maxByOrNull { it.dataCriacao }
                ultima?.let {
                    Toast.makeText(
                        this@TransactionActivity,
                        "Transação realizada: valor R$ ${it.valor}, método ${it.metodoConexao}, sincronizada: ${it.sincronizada}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (_: Exception) {}
        }
    }
}