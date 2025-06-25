package com.example.asyncpayments.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityProfileBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.model.UserResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.OfflineModeManager
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.SessionCacheUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("ProfileActivity", "onCreate chamado")
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(R.id.menu_profile).isChecked = true
        bottomNav.setOnItemSelectedListener { item ->
            AppLogger.log("ProfileActivity", "BottomNav item selecionado: ${item.itemId}")
            when (item.itemId) {
                R.id.menu_home -> {
                    AppLogger.log("ProfileActivity", "Navegando para HomeActivity")
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_add_funds -> {
                    AppLogger.log("ProfileActivity", "Navegando para AddFundsActivity")
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_transactions -> {
                    AppLogger.log("ProfileActivity", "Navegando para TransactionActivity")
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_profile -> {
                    AppLogger.log("ProfileActivity", "Já está em ProfileActivity")
                    true
                }
                else -> false
            }
        }

        AppLogger.log("ProfileActivity", "Chamando carregarPerfil()")
        carregarPerfil()

        binding.btnLogout.setOnClickListener {
            AppLogger.log("ProfileActivity", "Logout clicado")
            val dialogBinding = DialogResponseBinding.inflate(layoutInflater)
            dialogBinding.tvDialogTitle.text = "Sair"
            dialogBinding.tvDialogMessage.text = "Tem certeza que deseja sair?"
            dialogBinding.btnDialogOk.text = "Sim"
            dialogBinding.btnDialogOk.setOnClickListener {
                SharedPreferencesHelper(this).clearToken()
                AppLogger.log("ProfileActivity", "Token limpo no logout")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            val dialog = AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .setCancelable(true)
                .create()
            dialog.show()
        }
    }

    private fun carregarPerfil() {
        AppLogger.log("ProfileActivity", "carregarPerfil chamado. OfflineModeManager.isOffline = ${OfflineModeManager.isOffline}")
        if (OfflineModeManager.isOffline) {
            exibirPerfilDoCache()
            return
        }
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            try {
                AppLogger.log("ProfileActivity", "Buscando usuário online via API")
                val usuario = userService.buscarMeuUsuario()
                AppLogger.log("ProfileActivity", "Usuário online carregado: $usuario")
                if (usuario != null) {
                    preencherCamposPerfil(usuario)
                } else {
                    AppLogger.log("ProfileActivity", "Usuário online é nulo, exibindo cache")
                    exibirPerfilDoCache()
                }
            } catch (e: Exception) {
                AppLogger.log("ProfileActivity", "Erro ao carregar perfil online: ${e.message}, exibindo cache")
                exibirPerfilDoCache()
            }
        }
    }

    private fun exibirPerfilDoCache() {
        val cache = SessionCacheUtils.loadSessionCache(this)
        AppLogger.log("ProfileActivity", "Cache carregado: $cache")
        val usuario = cache?.usuario
        AppLogger.log("ProfileActivity", "Usuário do cache: $usuario")
        if (usuario != null) {
            preencherCamposPerfil(usuario)
        } else {
            exibirErro()
        }
    }

    private fun preencherCamposPerfil(usuario: UserResponse) {
        binding.tvProfileEmail.text = "Email: ${usuario.email}"
        binding.tvProfileName.text = "Nome: ${usuario.nome} ${usuario.sobrenome}"
        binding.tvProfileCpf.text = "CPF: ${usuario.cpf}"
        binding.tvProfileCelular.text = "Celular: ${usuario.celular}"
        binding.tvProfileRole.text = "Perfil: ${usuario.role}"

        val bloqueada = usuario.contaAssincrona?.bloqueada ?: false
        val status = if (bloqueada) "Bloqueada" else "Ativa"
        binding.tvProfileStatus.text = "Status da conta: $status"

        val syncDateUtcString = usuario.contaAssincrona?.ultimaSincronizacao
        if (!syncDateUtcString.isNullOrBlank()) {
            val syncInstant = java.time.Instant.parse(syncDateUtcString)
            val syncDateLocal = syncInstant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            val lastSyncFormatted = syncDateLocal.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            binding.tvProfileSyncDate.text = "Última sincronização: $lastSyncFormatted"
        } else {
            binding.tvProfileSyncDate.text = "Última sincronização: --"
        }
    }

    private fun exibirErro() {
        AppLogger.log("ProfileActivity", "exibirErro chamado")
        binding.tvProfileEmail.text = ""
        binding.tvProfileName.text = ""
        binding.tvProfileCpf.text = ""
        binding.tvProfileCelular.text = ""
        binding.tvProfileRole.text = ""
        binding.tvProfileStatus.text = ""
        binding.tvProfileSyncDate.text = ""
        // Opcional: você pode exibir uma mensagem de erro em algum campo ou Toast, se desejar
    }
}