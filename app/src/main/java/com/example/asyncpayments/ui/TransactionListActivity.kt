package com.example.asyncpayments.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asyncpayments.databinding.ActivityTransactionListBinding
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.TokenUtils
import com.example.asyncpayments.utils.carregarTransacoesUsuario
import kotlinx.coroutines.launch

class TransactionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionListBinding
    private var tipoConta: String? = null
    private var transacoes: List<TransactionResponse> = emptyList()
    private var adapter: TransactionAdapter? = null
    private lateinit var transactionService: TransactionService 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)

        tipoConta = intent.getStringExtra("tipoConta")
        carregarTransacoes(tipoConta)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSort.setOnClickListener {
            
            transacoes = transacoes.sortedByDescending { it.dataCriacao }
            val userId = TokenUtils.getUserIdFromToken(this)
            adapter = TransactionAdapter(transacoes, tipoConta, userId)
            binding.recyclerViewTransactions.adapter = adapter
        }
    }

    private fun carregarTransacoes(tipoConta: String?) {
        lifecycleScope.launch {
            try {
                val userId = TokenUtils.getUserIdFromToken(this@TransactionListActivity) ?: return@launch
                val transacoes = carregarTransacoesUsuario(transactionService, userId, tipoConta)
                adapter = TransactionAdapter(transacoes, tipoConta, userId)
                binding.recyclerViewTransactions.layoutManager = LinearLayoutManager(this@TransactionListActivity)
                binding.recyclerViewTransactions.adapter = adapter
            } catch (e: Exception) {
                Log.e("TransactionListActivity", "Erro ao carregar transações: ${e.message}")
                ShowNotification.show(
                    this@TransactionListActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar transações: ${e.message}"
                )
            }
        }
    }
}