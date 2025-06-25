package com.example.asyncpayments.ui

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityHomeBinding
import com.example.asyncpayments.domain.AccountManager
import com.example.asyncpayments.domain.ConnectionStatusMonitor
import com.example.asyncpayments.domain.TransactionManager
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineModeManager
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.SessionCacheUtils
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.TokenUtils
import com.example.asyncpayments.utils.carregarTransacoesUsuario
import com.example.asyncpayments.utils.isOnline
import com.example.asyncpayments.utils.sincronizarSeNecessario
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
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
    private lateinit var transactionManager: TransactionManager
    private lateinit var connectionMonitor: ConnectionStatusMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        val offlineQueue = OfflineTransactionQueue

        accountManager = AccountManager(userService)
        transactionManager = TransactionManager(transactionService, offlineQueue)
        connectionMonitor = ConnectionStatusMonitor(this) {
            lifecycleScope.launch {
                val userId = TokenUtils.getUserIdFromToken(this@HomeActivity) ?: return@launch
                val transactionService = RetrofitClient.getInstance(this@HomeActivity).create(TransactionService::class.java)
                sincronizarSeNecessario(transactionService, userId) {
                    // Envie a transação de sincronização automática aqui
                    val usuario = RetrofitClient.getInstance(this@HomeActivity).create(UserService::class.java).buscarMeuUsuario()
                    val saldoAsync = usuario?.contaAssincrona?.saldo ?: 0.0
                    if (saldoAsync > 0.0) {
                        val syncRequest = TransactionRequest(
                            idUsuarioOrigem = userId,
                            idUsuarioDestino = userId,
                            valor = saldoAsync,
                            tipoOperacao = "SINCRONIZACAO",
                            metodoConexao = "INTERNET",
                            gatewayPagamento = "INTERNO",
                            descricao = "Sincronização automática de saldo assíncrono"
                        )
                        transactionService.sendTransaction(syncRequest)
                        AppLogger.log("API", "sendTransaction(SINCRONIZACAO) automática enviada")
                    }
                }
            }
        }
        connectionMonitor.start()

        lifecycleScope.launch {
            val online = isOnline(this@HomeActivity)
            if (!online) {
                OfflineModeManager.isOffline = true
                val cache = SessionCacheUtils.loadSessionCache(this@HomeActivity)
                if (cache != null) {
                    syncBalanceValue = cache.usuario?.contaSincrona?.saldo ?: 0.0
                    asyncBalanceValue = cache.usuario?.contaAssincrona?.saldo ?: 0.0
                    updateSyncBalance()
                    updateAsyncBalance()
                } else {
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        "Sem dados offline disponíveis. Conecte-se à internet para atualizar o app."
                    )
                    binding.tvSyncBalance.text = "••••••••"
                    binding.tvAsyncBalance.text = "••••••••"
                }
                setupBalanceClickListeners()
                setupBottomNav()
                return@launch
            } else {
                OfflineModeManager.isOffline = false
            }

            carregarDadosUsuario()
            carregarSaldos()
            setupBalanceClickListeners()
            setupBottomNav()
        }

        setupSyncButton() // Certifique-se de chamar o setup do botão
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::connectivityManager.isInitialized) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        if (this::connectionMonitor.isInitialized) {
            connectionMonitor.stop()
        }
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
                syncBalanceValue = sync
                asyncBalanceValue = async
                updateSyncBalance()
                updateAsyncBalance()
                AppLogger.log("API", "carregarSaldos OK")
            },
            onError = { msg ->
                AppLogger.log("API", "carregarSaldos ERROR: $msg")
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar saldos: $msg"
                )
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

    private fun setupSyncButton() {
        binding.btnSyncAccounts.setOnClickListener {
            sincronizarContas()
        }
    }

    private suspend fun existeSincronizacaoPendente(transactionService: TransactionService, userId: Long): Boolean {
        return try {
            val transacoes = transactionService.listarTransacoes(
                tipoOperacao = "SINCRONIZACAO",
                idUsuarioOrigem = userId,
                idUsuarioDestino = userId
            )
            transacoes.any { 
                it.tipoOperacao == "SINCRONIZACAO" && it.status == "PENDENTE"
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun aguardarSincronizacaoConcluida(transactionService: TransactionService, userId: Long): Boolean {
        repeat(10) { // tenta por até 10 segundos
            val transacoes = transactionService.listarTransacoes(
                tipoOperacao = "SINCRONIZACAO",
                idUsuarioOrigem = userId,
                idUsuarioDestino = userId,
                status = "PENDENTE"
            )
            val syncPendente = transacoes.find { it.tipoOperacao == "SINCRONIZACAO" && it.status == "PENDENTE" }
            if (syncPendente == null) return true // Não há mais pendente, provavelmente já foi processada
            kotlinx.coroutines.delay(1000)
        }
        return false // Timeout
    }

    private fun sincronizarContas() {
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        val userId = TokenUtils.getUserIdFromToken(this) ?: return

        lifecycleScope.launch {
            try {
                if (existeSincronizacaoPendente(transactionService, userId)) {
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        "Já existe uma sincronização pendente. Aguarde o processamento."
                    )
                    return@launch
                }

                val usuario = userService.buscarMeuUsuario()
                val saldoAsync = usuario?.contaAssincrona?.saldo ?: 0.0
                if (saldoAsync <= 0.0) {
                    ShowNotification.show(this@HomeActivity, ShowNotification.Type.GENERIC, 0.0, "Nada para sincronizar.")
                    return@launch
                }

                val syncRequest = TransactionRequest(
                    idUsuarioOrigem = userId,
                    idUsuarioDestino = userId,
                    valor = saldoAsync,
                    tipoOperacao = "SINCRONIZACAO",
                    metodoConexao = "INTERNET",
                    gatewayPagamento = "INTERNO",
                    descricao = "Sincronização de saldo assíncrono"
                )

                transactionService.sendTransaction(syncRequest)
                AppLogger.log("API", "sendTransaction(SINCRONIZACAO) chamada realizada com sucesso")
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Sincronização enviada. Aguarde o processamento..."
                )

                val concluida = aguardarSincronizacaoConcluida(transactionService, userId)
                if (concluida) {
                    carregarSaldosUsuario()
                    AppLogger.log("API", "aguardarSincronizacaoConcluida OK")
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        "Sincronização concluída e saldos atualizados."
                    )
                } else {
                    AppLogger.log("API", "aguardarSincronizacaoConcluida TIMEOUT")
                    ShowNotification.show(
                        this@HomeActivity,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        "Sincronização enviada, mas ainda não processada. Atualize em instantes."
                    )
                }

                // Só aqui: sincroniza fila offline
                transactionManager.sincronizarTransacoesOffline(
                    context = this@HomeActivity,
                    scope = lifecycleScope,
                    buscarIdUsuarioPorEmail = ::buscarIdUsuarioPorEmail,
                    onResult = { sucesso, erro ->
                        // Notifique o usuário se quiser
                    }
                )
            } catch (e: Exception) {
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao sincronizar: ${e.message}"
                )
            }
        }
    }

    private fun carregarSaldosUsuario() {
        val token = SharedPreferencesHelper(this).getToken() ?: return
        val emailLogado = TokenUtils.getEmailFromToken(this) ?: return

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)

        lifecycleScope.launch {
            try {
                val usuario = userService.buscarMeuUsuario()
                if (usuario != null) {
                    val novoSaldoSync = usuario.contaSincrona?.saldo ?: 0.0
                    val novoSaldoAsync = usuario.contaAssincrona?.saldo ?: 0.0

                    if (novoSaldoSync != syncBalanceValue) {
                        syncBalanceValue = novoSaldoSync
                        updateSyncBalance()
                    }
                    if (novoSaldoAsync != asyncBalanceValue) {
                        asyncBalanceValue = novoSaldoAsync
                        updateAsyncBalance()
                    }
                } else {
                    binding.tvSyncBalance.text = "••••••••"
                    binding.tvAsyncBalance.text = "••••••••"
                }
            } catch (e: Exception) {
                AppLogger.log("HomeActivity", "Erro ao buscar dados do usuário: ${e.message}")
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar dados do usuário: ${e.message}"
                )
            }
        }
    }

    private fun sincronizarTransacoesOffline() {
        transactionManager.sincronizarTransacoesOffline(
            context = this,
            scope = lifecycleScope,
            buscarIdUsuarioPorEmail = ::buscarIdUsuarioPorEmail,
            onResult = { sucesso, erro ->
                // Opcional: notifique o usuário sobre o resultado
            }
        )
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
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        val userId = TokenUtils.getUserIdFromToken(this) ?: return

        lifecycleScope.launch {
            try {
                val usuario = userService.buscarMeuUsuario()
                val transacoesSync = carregarTransacoesUsuario(transactionService, userId, "SINCRONA")
                val transacoesAsync = carregarTransacoesUsuario(transactionService, userId, "ASSINCRONA")
                SessionCacheUtils.saveSessionCache(this@HomeActivity, usuario, transacoesSync, transacoesAsync)
                val listaUsuarios = userService.listarUsuarios()
                val prefs = getSharedPreferences("user_cache", MODE_PRIVATE)
                val gson = com.google.gson.Gson()
                prefs.edit().putString("usuarios", gson.toJson(listaUsuarios)).apply()
                syncBalanceValue = usuario?.contaSincrona?.saldo ?: 0.0
                asyncBalanceValue = usuario?.contaAssincrona?.saldo ?: 0.0
                updateSyncBalance()
                updateAsyncBalance()
            } catch (e: Exception) {
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar dados do usuário: ${e.message}"
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val tipoConta = intent.getStringExtra("tipoConta") ?: "SINCRONA"
        carregarTransacoes(tipoConta)
    }

    private fun buscarStatusTransacao(id: Long) {
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        lifecycleScope.launch {
            try {
                val status = transactionService.getTransactionStatus(id)
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Status da transação $id: $status"
                )
            } catch (e: Exception) {
                AppLogger.log("HomeActivity", "Erro ao buscar status da transação: ${e.message}")
                ShowNotification.show(
                    this@HomeActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao buscar status da transação: ${e.message}"
                )
            }
        }
    }

    private fun setupBalanceClickListeners() {
        binding.tvSyncBalance.setOnClickListener {
            abrirListaTransacoes("SINCRONA")
        }
        binding.tvAsyncBalance.setOnClickListener {
            abrirListaTransacoes("ASSINCRONA")
        }
    }

    private fun abrirListaTransacoes(tipoConta: String) {
        val intent = Intent(this, TransactionListActivity::class.java)
        intent.putExtra("tipoConta", tipoConta)
        startActivity(intent)
    }

    private fun carregarTransacoes(tipoConta: String) {
        val transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)
        val userId = TokenUtils.getUserIdFromToken(this) ?: return

        lifecycleScope.launch {
            try {
                val transacoes = carregarTransacoesUsuario(transactionService, userId, tipoConta)
                AppLogger.log("HomeActivity", "Transações carregadas: ${transacoes.size}")
            } catch (e: Exception) {
            }
        }
    }

    private fun setupBottomNav() {
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
    }
}