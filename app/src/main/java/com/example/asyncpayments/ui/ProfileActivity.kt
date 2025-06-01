package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityProfileBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(com.example.asyncpayments.R.id.menu_profile).isChecked = true
        bottomNav.setOnItemSelectedListener { item ->
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
                com.example.asyncpayments.R.id.menu_transactions -> {
                    startActivity(Intent(this, TransactionActivity::class.java))
                    finish()
                    true
                }
                com.example.asyncpayments.R.id.menu_profile -> true
                else -> false
            }
        }

        carregarPerfil()

        binding.btnLogout.setOnClickListener {
            val dialogBinding = DialogResponseBinding.inflate(layoutInflater)
            dialogBinding.tvDialogTitle.text = "Sair"
            dialogBinding.tvDialogMessage.text = "Tem certeza que deseja sair?"
            dialogBinding.btnDialogOk.text = "Sim"
            dialogBinding.btnDialogOk.setOnClickListener {
                
                SharedPreferencesHelper(this).clearToken()
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
        val token = SharedPreferencesHelper(this).getToken() ?: return
        val emailLogado = getEmailFromToken(token) ?: return

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            try {
                val usuario = userService.getMe()
                if (usuario != null) {
                    binding.tvProfileEmail.text = "Email: ${usuario.email}"
                    binding.tvProfileName.text = "Nome: ${usuario.nome} ${usuario.sobrenome}"
                    binding.tvProfileCpf.text = "CPF: ${usuario.cpf}"
                    binding.tvProfileCelular.text = "Celular: ${usuario.celular}"
                    binding.tvProfileRole.text = "Perfil: ${usuario.role}"

                    val bloqueada = usuario.contaAssincrona?.bloqueada ?: false
                    val status = if (bloqueada) "Bloqueada" else "Ativa"
                    binding.tvProfileStatus.text = "Status da conta: $status"
                    val statusColor = if (bloqueada) {
                        getColor(R.color.red_accent)
                    } else {
                        getColor(R.color.green_active)
                    }
                    binding.ivVerifiedUser.setColorFilter(statusColor)
                    binding.tvProfileStatus.setTextColor(statusColor)

                    val syncDateUtcString = usuario.contaAssincrona?.ultimaSincronizacao
                    if (!syncDateUtcString.isNullOrBlank()) {
                        val syncInstant = if (syncDateUtcString.endsWith("Z")) {
                            Instant.parse(syncDateUtcString)
                        } else {
                            LocalDateTime.parse(syncDateUtcString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                .atZone(ZoneId.of("UTC")).toInstant()
                        }
                        val syncDateLocal = syncInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()

                        
                        val (syncStatus, syncColor) = getSyncStatusText(syncDateUtcString!!)
                        binding.tvProfileSync.text = syncStatus
                        binding.tvProfileSync.setTextColor(getColor(syncColor))

                        
                        val lastSyncFormatted = syncDateLocal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        binding.tvProfileSyncDate.text = "Última sincronização: $lastSyncFormatted"
                    } else {
                        binding.tvProfileSync.text = "Sincronização: --"
                        binding.tvProfileSyncDate.text = "Última sincronização: --"
                    }
                } else {
                    binding.tvProfileEmail.text = "Email: --"
                    binding.tvProfileName.text = "Nome: --"
                    binding.tvProfileCpf.text = "CPF: --"
                    binding.tvProfileCelular.text = "Celular: --"
                    binding.tvProfileRole.text = "Perfil: --"
                    binding.tvProfileStatus.text = "Status da conta: --"
                    binding.tvProfileSync.text = "Última sincronização: --"
                }
            } catch (e: Exception) {
                ShowNotification.show(
                    this@ProfileActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao carregar perfil: ${e.message}"
                )
            }
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

    private fun getSyncStatusText(syncDateUtcString: String): Pair<String, Int> {
        
        val syncInstant = if (syncDateUtcString.endsWith("Z")) {
            Instant.parse(syncDateUtcString)
        } else {
            LocalDateTime.parse(syncDateUtcString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.of("UTC")).toInstant()
        }
        val syncDateLocal = syncInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val now = LocalDateTime.now()
        val duration = Duration.between(syncDateLocal, now)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val days = duration.toDays()

        val statusText = when {
            hours >= 24 -> "Sincronizado há $days dias"
            hours > 0 -> "Sincronizado há $hours horas"
            minutes > 0 -> "Sincronizado há $minutes minutos"
            else -> "Sincronizado agora"
        }

        val color = when {
            hours >= 72 -> R.color.red_accent 
            hours >= 60 -> R.color.yellow_accent 
            else -> R.color.green_active 
        }

        return Pair(statusText, color)
    }
}