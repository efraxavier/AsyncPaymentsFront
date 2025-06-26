package com.example.asyncpayments.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asyncpayments.databinding.ActivityTransactionListBinding
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.model.UserResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.TokenUtils
import com.example.asyncpayments.utils.carregarTransacoesUsuario
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min

class TransactionListActivity : AppCompatActivity() {
    private var tipoConta: String? = null
    private var transacoes: List<TransactionResponse> = emptyList()
    private var transacoesSincronas: List<TransactionResponse> = emptyList()
    private var transacoesAssincronas: List<TransactionResponse> = emptyList()
    private var contaSelecionada: String = "SINCRONA"
    private var adapter: TransactionAdapter? = null
    private lateinit var transactionService: TransactionService

    private var currentPage = 0
    private val pageSize = 10
    private var totalPages = 1

    private var userId: Long? = null

    private var carregandoTransacoes = false

    private var usuarios: List<UserResponse>? = null

    private lateinit var binding: ActivityTransactionListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicialize o OfflineTransactionQueue se necessário
        val db = com.example.asyncpayments.data.AppDatabase.getInstance(this)
        com.example.asyncpayments.utils.OfflineTransactionQueue.init(db.offlineTransactionDao())

        binding = ActivityTransactionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tipoConta = intent.getStringExtra("tipoConta") ?: "SINCRONA"
        userId = TokenUtils.getUserIdFromToken(this) ?: return

        binding.recyclerViewTransactions.layoutManager = LinearLayoutManager(this)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) {
                currentPage++
                atualizarListaPorConta()
            }
        }
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                atualizarListaPorConta()
            }
        }

        carregarUsuarios {
            carregarTransacoes()
        }
    }

    private fun carregarUsuarios(onLoaded: () -> Unit) {
        val prefs = getSharedPreferences("user_cache", MODE_PRIVATE)
        val gson = Gson()
        val usuariosJson = prefs.getString("usuarios", null)
        usuarios = if (usuariosJson != null) {
            gson.fromJson(usuariosJson, Array<UserResponse>::class.java)?.toList()
        } else {
            emptyList()
        }
        onLoaded()
    }

    private fun carregarTransacoes() {
        if (carregandoTransacoes) return
        carregandoTransacoes = true

        lifecycleScope.launch {
            try {
                val transactionService = RetrofitClient.getInstance(this@TransactionListActivity).create(TransactionService::class.java)
                val transacoesSync = carregarTransacoesUsuario(transactionService, userId ?: 0L, "SINCRONA")
                val transacoesAsync = carregarTransacoesUsuario(transactionService, userId ?: 0L, "ASSINCRONA")

                val transacoesOffline = OfflineTransactionQueue.loadAll(this@TransactionListActivity)
                    .map {
                        val usuarioOrigem = usuarios?.find { u -> u.id.toString() == it.origem }
                        val usuarioDestino = usuarios?.find { u -> u.id.toString() == it.destino }
                        TransactionResponse(
                            id = -1L,
                            idUsuarioOrigem = it.origem.toLongOrNull() ?: 0L,
                            idUsuarioDestino = it.destino.toLongOrNull() ?: 0L,
                            valor = it.valor,
                            tipoTransacao = "ASSINCRONA",
                            tipoOperacao = "ASSINCRONA",
                            metodoConexao = it.metodoConexao,
                            gatewayPagamento = it.gatewayPagamento,
                            descricao = it.descricao,
                            dataCriacao = it.dataCriacao.toString(),
                            dataAtualizacao = it.dataCriacao.toString(),
                            sincronizada = false,
                            status = "PENDENTE",
                            nomeUsuarioOrigem = usuarioOrigem?.nome ?: "",
                            emailUsuarioOrigem = usuarioOrigem?.email ?: "",
                            cpfUsuarioOrigem = usuarioOrigem?.cpf ?: "",
                            nomeUsuarioDestino = usuarioDestino?.nome ?: "",
                            emailUsuarioDestino = usuarioDestino?.email ?: "",
                            cpfUsuarioDestino = usuarioDestino?.cpf ?: "",
                            dataSincronizacaoOrigem = null,
                            dataSincronizacaoDestino = null,
                            identificadorOffline = it.identificadorOffline
                        )
                    }

                val todasTransacoes = transacoesOffline + transacoesSync + transacoesAsync

                transacoesSincronas = todasTransacoes.filter { it.tipoTransacao == "SINCRONA" }
                transacoesAssincronas = todasTransacoes.filter { it.tipoTransacao == "ASSINCRONA" }

                contaSelecionada = tipoConta ?: "SINCRONA"
                currentPage = 0
                atualizarListaPorConta()

                AppLogger.log("TransactionListActivity", "Exibindo transações: offline=${transacoesOffline.size}, sync=${transacoesSync.size}, async=${transacoesAsync.size}")
            } finally {
                carregandoTransacoes = false
            }
        }
    }

    private fun atualizarListaPorConta() {
        val userEmail = TokenUtils.getEmailFromToken(this)
        val lista = if (contaSelecionada == "SINCRONA") transacoesSincronas else transacoesAssincronas

        // Filtra apenas transações do usuário logado
        val transacoesFiltradas = lista.filter {
            it.idUsuarioOrigem == userId || it.idUsuarioDestino == userId
        }.sortedByDescending { it.dataCriacao.toLongOrNull() ?: 0L }

        // Paginação
        val start = currentPage * pageSize
        val end = min(start + pageSize, transacoesFiltradas.size)
        val pagina = if (start < transacoesFiltradas.size) transacoesFiltradas.subList(start, end) else emptyList()

        adapter = TransactionAdapter(pagina, contaSelecionada, userEmail)
        binding.recyclerViewTransactions.adapter = adapter

        totalPages = ceil(transacoesFiltradas.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
        binding.tvPageInfo.text = "Página ${currentPage + 1} de $totalPages"
        binding.btnPrevPage.isEnabled = currentPage > 0
        binding.btnNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun ordenarTransacoesPorLogica(transacoes: List<TransactionResponse>): List<TransactionResponse> {
        fun isGatewayInternet(gateway: String) = gateway in listOf("STRIPE", "PAGARME", "MERCADO_PAGO")
        fun isGatewayOffline(gateway: String) = gateway in listOf("PAYCERTIFY", "DREX", "PAGSEGURO")
        fun isMetodoOffline(metodo: String) = metodo in listOf("BLUETOOTH", "SMS", "NFC")

        val sincronizacaoSincrona = transacoes.filter {
            it.tipoTransacao == "SINCRONA" && it.tipoOperacao == "SINCRONIZACAO" &&
            it.metodoConexao == "INTERNET" && it.gatewayPagamento == "INTERNO"
        }.sortedByDescending { it.dataCriacao }

        val adicaoFundosSincrona = transacoes.filter {
            it.tipoTransacao == "SINCRONA" && it.tipoOperacao == "INTERNA" &&
            it.metodoConexao == "ASYNC" && it.gatewayPagamento == "INTERNO"
        }.sortedByDescending { it.dataCriacao }

        val transferenciaSincrona = transacoes.filter {
            it.tipoTransacao == "SINCRONA" && it.tipoOperacao == "SINCRONA" &&
            it.metodoConexao == "INTERNET" && isGatewayInternet(it.gatewayPagamento)
        }.sortedByDescending { it.dataCriacao }

        val sincronizacaoAssincrona = transacoes.filter {
            it.tipoTransacao == "ASSINCRONA" && it.tipoOperacao == "SINCRONIZACAO" &&
            it.metodoConexao == "INTERNET" && it.gatewayPagamento == "INTERNO"
        }.sortedByDescending { it.dataCriacao }

        val adicaoFundosAssincrona = transacoes.filter {
            it.tipoTransacao == "ASSINCRONA" && it.tipoOperacao == "INTERNA" &&
            it.metodoConexao == "ASYNC" && it.gatewayPagamento == "INTERNO"
        }.sortedByDescending { it.dataCriacao }

        val transferenciaAssincrona = transacoes.filter {
            it.tipoTransacao == "ASSINCRONA" && it.tipoOperacao == "ASSINCRONA" &&
            isMetodoOffline(it.metodoConexao) && isGatewayOffline(it.gatewayPagamento)
        }.sortedByDescending { it.dataCriacao }

        // Junte os grupos na ordem da tabela:
        return sincronizacaoSincrona +
            adicaoFundosSincrona +
            transferenciaSincrona +
            sincronizacaoAssincrona +
            adicaoFundosAssincrona +
            transferenciaAssincrona
    }

    private fun criarPagamento(
        valor: Double,
        metodoConexao: String,
        gatewayPagamento: String,
        descricao: String,
        identificadorOffline: String?
    ): PaymentData {
        val userIdOrigem = TokenUtils.getUserIdFromToken(this)
        val tipoContaDestino = this@TransactionListActivity.tipoConta ?: "SINCRONA"
        // Exemplo: escolha o primeiro usuário diferente do atual
        val idUsuarioDestino = usuarios?.firstOrNull { it.id != userId }?.id

        return PaymentData(
            id = System.currentTimeMillis(),
            valor = valor,
            origem = userIdOrigem!!.toString(),
            destino = idUsuarioDestino?.toString() ?: "",
            data = System.currentTimeMillis().toString(),
            metodoConexao = metodoConexao,
            gatewayPagamento = gatewayPagamento,
            descricao = descricao,
            dataCriacao = System.currentTimeMillis(),
            identificadorOffline = identificadorOffline.toString()
        )
    }

    private fun comparaTipoConta(tipoA: String?, tipoB: String?): Boolean {
        return (tipoA ?: "SINCRONA") == (tipoB ?: "SINCRONA")
    }
}