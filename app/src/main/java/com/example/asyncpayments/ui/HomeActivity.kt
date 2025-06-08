package com.example.asyncpayments.ui

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityHomeBinding
import com.example.asyncpayments.domain.AccountManager
import com.example.asyncpayments.domain.AccountSyncManager
import com.example.asyncpayments.domain.ConnectionStatusMonitor
import com.example.asyncpayments.domain.TransactionManager
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.SyncService
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.NotificationQueue
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.http.GET
import retrofit2.http.Query

class HomeActivity : AppCompatActivity() {
    companion object {
        
        private var primeiraSincronizacaoFeita = false
    }

    private lateinit var binding: ActivityHomeBinding

    
    private var syncVisible = true
    private var asyncVisible = true
    private var syncBalanceValue: Double = 0.0
    private var asyncBalanceValue: Double = 0.0

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private var primeiraSincronizacao = true

    private lateinit var accountManager: AccountManager
    private lateinit var syncManager: AccountSyncManager
    private lateinit var transactionManager: TransactionManager
    private lateinit var connectionMonitor: ConnectionStatusMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val token = SharedPreferencesHelper(this).getToken()
        val email = token?.let { getEmailFromToken(it) } ?: ""
        val nomeUsuario = if (email.contains("@")) email.substringBefore("@") else email
        binding.tvWelcome.text = "Bem-vindo $nomeUsuario"
        setContentView(binding.root)

        
        val pendentes = NotificationQueue.getAndClearAll()
        Log.d("HomeActivity", "Notificacoes pendentes para exibir: $pendentes")
        pendentes.forEach { transacao ->
            val descricao = if (!transacao.descricao.isNullOrBlank()) "\nDescrição: ${transacao.descricao}" else ""
            val mensagem = "Método: ${transacao.metodoConexao}\n" +
                           "Recebido de ID: ${transacao.idUsuarioOrigem}\n" +
                           "Valor: R$ %.2f".format(transacao.valor) +
                           descricao
            ShowNotification.show(
                this,
                ShowNotification.Type.TRANSACTION_RECEIVED,
                transacao.valor,
                mensagem
            )
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(R.id.menu_home).isChecked = true
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> true
                R.id.menu_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_transactions -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        
        binding.btnToggleSyncBalance.setOnClickListener {
            syncVisible = !syncVisible
            updateSyncBalance()
        }

        
        binding.btnToggleAsyncBalance.setOnClickListener {
            asyncVisible = !asyncVisible
            updateAsyncBalance()
        }

        
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        val syncService = RetrofitClient.getInstance(this).create(SyncService::class.java)
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        accountManager = AccountManager(userService)
        syncManager = AccountSyncManager(syncService)
        transactionManager = TransactionManager(transactionService, OfflineTransactionQueue)
        connectionMonitor = ConnectionStatusMonitor(this) {
            sincronizarTudo()
        }
        connectionMonitor.start()

        
        carregarSaldos()

        binding.btnSyncAccounts.setOnClickListener {
            sincronizarContas()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        connectionMonitor.stop()
    }

    private fun updateSyncBalance() {
        if (syncVisible) {
            binding.tvSyncBalance.text = "R$ %.2f".format(syncBalanceValue)
            binding.btnToggleSyncBalance.setImageResource(R.drawable.ic_eye)
        } else {
            binding.tvSyncBalance.text = "••••••••"
            binding.btnToggleSyncBalance.setImageResource(R.drawable.ic_eye_off)
        }
    }

    private fun updateAsyncBalance() {
        if (asyncVisible) {
            binding.tvAsyncBalance.text = "R$ %.2f".format(asyncBalanceValue)
            binding.btnToggleAsyncBalance.setImageResource(R.drawable.ic_eye)
        } else {
            binding.tvAsyncBalance.text = "••••••••"
            binding.btnToggleAsyncBalance.setImageResource(R.drawable.ic_eye_off)
        }
    }

    private fun carregarSaldos() {
        accountManager.carregarSaldos(
            lifecycleScope,
            onResult = { sync, async ->
                
            },
            onError = { msg ->
                
            }
        )
    }

    private fun sincronizarTudo() {
        transactionManager.sincronizarTransacoesOffline(
            context = this,
            scope = lifecycleScope,
            buscarIdUsuarioPorEmail = ::buscarIdUsuarioPorEmail,
            onResult = { sucesso, erro -> }
        )
    }

    private fun sincronizarContas() {
        val retrofit = RetrofitClient.getInstance(this)
        lifecycleScope.launch {
            try {
                val response = retrofit.create(SyncService::class.java).sincronizarMinhaConta()
                val message = response.string()
                
                if (!primeiraSincronizacaoFeita) {
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.SYNC_SUCCESS,
                        0.0,
                        "$message"
                    )
                    primeiraSincronizacaoFeita = true
                }
                carregarSaldosUsuario()
            } catch (e: Exception) {
                if (!primeiraSincronizacaoFeita) {
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.SYNC_ERROR,
                        0.0,
                        "${e.message}"
                    )
                    primeiraSincronizacaoFeita = true
                }
            }
        }
    }

    private fun carregarSaldosUsuario() {
        val token = SharedPreferencesHelper(this).getToken() ?: return
        val emailLogado = getEmailFromToken(token) ?: return

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)

        lifecycleScope.launch {
            try {
                val usuario = userService.getMe()
                if (usuario != null) {
                    val novoSaldoSync = usuario.contaSincrona?.saldo ?: 0.0
                    val novoSaldoAsync = usuario.contaAssincrona?.saldo ?: 0.0

                    // Atualizar saldo na interface apenas se houver alteração
                    if (novoSaldoSync != syncBalanceValue) {
                        syncBalanceValue = novoSaldoSync
                        updateSyncBalance()
                    }
                    if (novoSaldoAsync != asyncBalanceValue) {
                        asyncBalanceValue = novoSaldoAsync
                        updateAsyncBalance()
                    }

                    // Buscar transações recebidas e notificar
                    val transacoesRecebidas = transactionService.getReceivedTransactions()
                    val transacaoMaisRecente = transacoesRecebidas.maxByOrNull { it.dataCriacao }
                    if (transacaoMaisRecente != null && transacaoMaisRecente.valor == asyncBalanceValue) {
                        val mensagem = "Método: ${transacaoMaisRecente.metodoConexao}\n" +
                                       "Recebido de ID: ${transacaoMaisRecente.idUsuarioOrigem}\n" +
                                       "Valor: R$ %.2f".format(transacaoMaisRecente.valor) +
                                       if (!transacaoMaisRecente.descricao.isNullOrBlank()) "\nDescrição: ${transacaoMaisRecente.descricao}" else ""
                        ShowNotification.show(
                            this@HomeActivity,
                            ShowNotification.Type.TRANSACTION_RECEIVED,
                            transacaoMaisRecente.valor,
                            mensagem
                        )
                    }
                } else {
                    binding.tvSyncBalance.text = "••••••••"
                    binding.tvAsyncBalance.text = "••••••••"
                }
            } catch (e: Exception) {
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar saldos: ${e.message}"
                )
            }
        }
    }

    private fun getUserIdFromToken(): Long? {
        val token = SharedPreferencesHelper(this).getToken() ?: return null
        return try {
            val payload = android.util.Base64.decode(token.split(".")[1], android.util.Base64.DEFAULT)
            val json = org.json.JSONObject(String(payload))
            json.getLong("id")
        } catch (e: Exception) {
            null
        }
    }

    private fun getEmailFromToken(token: String): String? {
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getString("sub")
        } catch (e: Exception) {
            null
        }
    }

    private fun sincronizarTransacoesOffline() {
        val queue = OfflineTransactionQueue.loadAll(this)
        if (queue.isEmpty()) {
            return
        }
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            var sucesso = 0
            var erro = 0
            for (transacao in queue) {
                try {
                    val idUsuarioOrigem = buscarIdUsuarioPorEmail(transacao.origem)
                    val idUsuarioDestino = buscarIdUsuarioPorEmail(transacao.destino)
                    if (idUsuarioOrigem != null && idUsuarioDestino != null) {
                        val request = TransactionRequest(
                            idUsuarioOrigem = idUsuarioOrigem,
                            idUsuarioDestino = idUsuarioDestino,
                            valor = transacao.valor,
                            metodoConexao = transacao.metodoConexao,
                            gatewayPagamento = transacao.gatewayPagamento,
                            descricao = transacao.descricao
                        )
                        transactionService.sendTransaction(request)
                        sucesso++
                    } else {
                        erro++
                    }
                } catch (e: Exception) {
                    erro++
                }
            }
            OfflineTransactionQueue.clear(this@HomeActivity)
            
            ShowNotification.show(
                this@HomeActivity,
                ShowNotification.Type.SYNC_SUCCESS,
                0.0,
                "Transações sincronizadas: $sucesso sucesso(s), $erro erro(s)."
            )
        }
    }

    
    interface UserIdService {
        @GET("/auth/user/id")
        suspend fun getIdByEmail(@Query("email") email: String): Long
    }

    
    private suspend fun buscarIdUsuarioPorEmail(email: String): Long? {
        return try {
            val service = RetrofitClient.getInstance(this).create(UserIdService::class.java)
            service.getIdByEmail(email)
        } catch (e: Exception) {
            null
        }
    }

    private fun carregarDadosUsuario() {
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)

        lifecycleScope.launch {
            try {
                val usuario = userService.getMe()
                if (usuario != null) {
                    binding.tvWelcome.text = "Bem-vindo, ${usuario.nome} ${usuario.sobrenome}"
                    syncBalanceValue = usuario.contaSincrona?.saldo ?: 0.0
                    asyncBalanceValue = usuario.contaAssincrona?.saldo ?: 0.0

                    // Atualizar os saldos com base na visibilidade
                    updateSyncBalance()
                    updateAsyncBalance()
                } else {
                    binding.tvSyncBalance.text = "••••••••"
                    binding.tvAsyncBalance.text = "••••••••"
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Erro ao buscar dados do usuário: ${e.message}")
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar dados do usuário: ${e.message}"
                )
            }
        }
    }

    // Comentando o fluxo de notificações
    /*
    private fun checarTransacoesRecebidasENotificar() {
        val prefs = getSharedPreferences("notificacoes", MODE_PRIVATE)
        val idsNotificados = prefs.getStringSet("ids_transacoes_recebidas", emptySet())?.toMutableSet() ?: mutableSetOf()

        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            try {
                val recebidas = transactionService.getReceivedTransactions()
                Log.d("HomeActivity", "Transacoes recebidas do backend: $recebidas")
                for (transacao in recebidas) {
                    if (!idsNotificados.contains(transacao.id.toString())) {
                        NotificationQueue.add(transacao)
                        Log.d("HomeActivity", "Transacao adicionada na fila: $transacao")
                        idsNotificados.add(transacao.id.toString())
                    }
                }
                prefs.edit().putStringSet("ids_transacoes_recebidas", idsNotificados).apply()
            } catch (e: Exception) {
                Log.e("HomeActivity", "Erro ao buscar transacoes: ${e.message}")
            }
        }
    }
    */

    override fun onResume() {
        super.onResume()
        carregarDadosUsuario()
        // checarTransacoesRecebidasENotificar() // Comentado para evitar problemas
    }

    private fun atualizarSaldoAssincrono() {
        val retrofit = RetrofitClient.getInstance(this)
        val syncService = retrofit.create(SyncService::class.java)
        val userService = retrofit.create(UserService::class.java)
        lifecycleScope.launch {
            try {
                
                syncService.sincronizarMinhaConta()
                
                val usuario = userService.getMe()
                val saldo = usuario.contaAssincrona?.saldo ?: 0.0
                
                binding.tvAsyncBalance.text = "R$ %.2f".format(saldo)
            } catch (e: Exception) {
                
            }
        }
    }

    private fun setupSyncButton() {
        binding.btnSyncAccounts.setOnClickListener {
            syncManager.sincronizarMinhaConta(
                lifecycleScope,
                onSuccess = { ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Conta sincronizada") },
                onError = { msg -> ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, msg) }
            )
        }
    }

    private fun setupAutoTransactionSync() {
        this.connectionMonitor.onConnected = {
            transactionManager.sincronizarTransacoesOffline(
                context = this,
                scope = lifecycleScope,
                buscarIdUsuarioPorEmail = ::buscarIdUsuarioPorEmail,
                onResult = { sucesso, erro ->
                    ShowNotification.show(
                        this,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        "Transações sincronizadas: $sucesso sucesso(s), $erro erro(s)."
                    )
                }
            )
        }
    }
}