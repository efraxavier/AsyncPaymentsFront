package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityProfileBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    }

    private fun carregarPerfil() {
    val token = SharedPreferencesHelper(this).getToken() ?: return
    val emailLogado = getEmailFromToken(token) ?: return

    val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
    lifecycleScope.launch {
        try {
            val usuarios = userService.listarUsuarios()
            val usuario = usuarios.find { it.email == emailLogado }
            if (usuario != null) {
                binding.tvProfileEmail.text = "Email: ${usuario.email}"
                binding.tvProfileName.text = "Nome: ${usuario.nome} ${usuario.sobrenome}"
                binding.tvProfileCpf.text = "CPF: ${usuario.cpf}"
                binding.tvProfileCelular.text = "Celular: ${usuario.celular}"
                binding.tvProfileFormaPagamento.text = "Forma de Pagamento: ${usuario.formaPagamentoAlternativa}"
                binding.tvProfileRole.text = "Perfil: ${usuario.role}"

                val bloqueada = usuario.contaAssincrona.bloqueada
                val status = if (bloqueada) "Bloqueada" else "Ativa"
                binding.tvProfileStatus.text = "Status da conta: $status"
                val statusColor = if (bloqueada) {
                    getColor(R.color.red_accent)
                } else {
                    getColor(R.color.green_active)
                }
                binding.ivVerifiedUser.setColorFilter(statusColor)
                binding.tvProfileStatus.setTextColor(statusColor)
                binding.tvProfileSync.text = "Última sincronização: ${usuario.contaAssincrona.ultimaSincronizacao}"
            } else {
                binding.tvProfileEmail.text = "Email: --"
                binding.tvProfileName.text = "Nome: --"
                binding.tvProfileCpf.text = "CPF: --"
                binding.tvProfileCelular.text = "Celular: --"
                binding.tvProfileFormaPagamento.text = "Forma de Pagamento: --"
                binding.tvProfileRole.text = "Perfil: --"
                binding.tvProfileStatus.text = "Status da conta: --"
                binding.tvProfileSync.text = "Última sincronização: --"
            }
        } catch (e: Exception) {
            showCustomDialog("Erro", "Erro ao carregar perfil: ${e.message}")
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

    private fun showCustomDialog(title: String, message: String) {
        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
            .show()
    }
}