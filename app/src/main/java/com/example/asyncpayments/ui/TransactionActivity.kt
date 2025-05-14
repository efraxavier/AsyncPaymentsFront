package com.example.asyncpayments.ui

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
                val response = transactionService.sendTransaction(request)
                Toast.makeText(this@TransactionActivity, "Transação realizada com sucesso", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@TransactionActivity, "Erro ao realizar transação: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}