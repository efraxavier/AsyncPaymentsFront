package com.example.asyncpayments.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asyncpayments.databinding.ActivityTransactionListBinding
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineModeManager
import com.example.asyncpayments.utils.SessionCacheUtils
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.TokenUtils
import com.example.asyncpayments.utils.carregarTransacoesUsuario
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

class TransactionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionListBinding
    private var tipoConta: String? = null
    private var transacoes: List<TransactionResponse> = emptyList()
    private var adapter: TransactionAdapter? = null
    private lateinit var transactionService: TransactionService

    private var currentPage = 0
    private val pageSize = 10
    private var totalPages = 1

    private var userId: Long? = null

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

        binding.recyclerViewTransactions.layoutManager = LinearLayoutManager(this)

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                exibirPagina()
            }
        }
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                exibirPagina()
            }
        }

        userId = TokenUtils.getUserIdFromToken(this)
    }

    private fun carregarTransacoes(tipoConta: String?) {
        lifecycleScope.launch {
            if (OfflineModeManager.isOffline) {
                val cache = SessionCacheUtils.loadSessionCache(this@TransactionListActivity)
                val lista = when (tipoConta) {
                    "SINCRONA" -> cache?.transacoesSync ?: emptyList()
                    "ASSINCRONA" -> cache?.transacoesAsync ?: emptyList()
                    else -> emptyList()
                }
                transacoes = lista.distinctBy { it.id ?: it.dataCriacao }
                    .sortedByDescending { it.dataCriacao }
                totalPages = ceil(transacoes.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
                currentPage = 0
                exibirPagina()
                return@launch
            }

            try {
                val lista = carregarTransacoesUsuario(
                    transactionService,
                    userId ?: return@launch, // ou trate o caso de userId nulo
                    tipoConta
                )
                transacoes = lista.distinctBy { it.id ?: it.dataCriacao }
                    .sortedByDescending { it.dataCriacao }
                totalPages = ceil(transacoes.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
                currentPage = 0
                exibirPagina()
            } catch (e: Exception) {
                ShowNotification.show(
                    this@TransactionListActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar transações: ${e.message}"
                )
            }
        }
    }

    private fun exibirPagina() {
        val userEmail = TokenUtils.getEmailFromToken(this)
        val start = currentPage * pageSize
        val end = min(start + pageSize, transacoes.size)
        val pagina = if (start < transacoes.size) transacoes.subList(start, end) else emptyList()
        AppLogger.log("TransactionListActivity", "Setando adapter com pagina.size = ${pagina.size}")
        adapter = TransactionAdapter(pagina, tipoConta, userEmail)
        binding.recyclerViewTransactions.adapter = adapter

        // Atualiza texto de paginação
        binding.tvPageInfo.text = "Página ${currentPage + 1} de $totalPages"

        // Habilita/desabilita botões
        binding.btnPrevPage.isEnabled = currentPage > 0
        binding.btnNextPage.isEnabled = currentPage < totalPages - 1

        AppLogger.log("TransactionListActivity", "transacoes.size = ${transacoes.size}")
        AppLogger.log("TransactionListActivity", "pagina.size = ${pagina.size}")
    }
}