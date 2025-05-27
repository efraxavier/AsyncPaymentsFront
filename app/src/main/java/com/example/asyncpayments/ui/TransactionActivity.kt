package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityTransactionBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.UserResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.SharedPreferencesHelper
import kotlinx.coroutines.launch
import org.json.JSONObject

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private var userIdOrigem: Long? = null
    private var usuarios: List<UserResponse>? = null

    private val metodosConexao = listOf("INTERNET", "BLUETOOTH", "SMS", "NFC")
    private val gatewaysInternet = listOf("STRIPE", "PAGARME", "MERCADO_PAGO", "INTERNO")
    private val gatewaysOffline = listOf("DREX", "PAGSEGURO", "PAYCERTIFY")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userIdOrigem = getUserIdFromToken()
        binding.etIdUsuarioOrigem.setText(userIdOrigem?.toString() ?: "")
        binding.etIdUsuarioOrigem.isEnabled = false

        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        // Preencher lista de usuários
        lifecycleScope.launch {
            try {
                usuarios = userService.listarUsuarios()
                val userList = usuarios?.map { "${it.email} (ID: ${it.id})" } ?: emptyList()
                val adapter = ArrayAdapter(
                    this@TransactionActivity,
                    R.layout.item_dropdown_orange, // use o layout customizado
                    userList
                )
                binding.etIdUsuarioDestino.setAdapter(adapter)
            } catch (e: Exception) {
                showCustomDialog("Erro", "Erro ao carregar usuários: ${e.message}")
            }
        }

        // Listener para seleção de usuário de destino
        binding.etIdUsuarioDestino.setOnItemClickListener { _, _, _, _ ->
            setupMetodoConexaoDropdown()
        }

        // Dropdown para método de conexão
        val metodoAdapter = ArrayAdapter(
            this,
            R.layout.item_dropdown_orange,
            metodosConexao
        )
        binding.actvMetodoConexao.setAdapter(metodoAdapter)
        binding.actvMetodoConexao.setOnItemClickListener { parent, _, position, _ ->
            val metodo = parent.getItemAtPosition(position) as String
            setupGatewayDropdown(metodo)
        }

        binding.btnSendTransaction.setOnClickListener {
            val valor = binding.etAmount.text.toString().toDoubleOrNull()
            val idUsuarioDestino = getSelectedUserId()
            val metodoConexao = binding.actvMetodoConexao.text.toString()
            val gatewayPagamento = binding.actvGatewayPagamento.text.toString()

            if (valor != null && userIdOrigem != null && idUsuarioDestino != null &&
                metodoConexao.isNotBlank() && gatewayPagamento.isNotBlank()) {

                try {
                    if (metodoConexao == "INTERNET") {
                        if (gatewayPagamento !in gatewaysInternet) {
                            throw IllegalArgumentException("Para transações via INTERNET, só são permitidos os gateways: STRIPE, PAGARME, MERCADO_PAGO e INTERNO.")
                        }
                        sendTransaction(userIdOrigem!!, idUsuarioDestino, valor, metodoConexao, gatewayPagamento)
                    } else if (metodoConexao in listOf("SMS", "NFC", "BLUETOOTH")) {
                        if (gatewayPagamento !in gatewaysOffline) {
                            throw IllegalArgumentException("Para transações offline (SMS, NFC, BLUETOOTH), só são permitidos os gateways: DREX, PAGSEGURO e PAYCERTIFY.")
                        }
                        sendTransaction(userIdOrigem!!, idUsuarioDestino, valor, metodoConexao, gatewayPagamento)
                    } else {
                        throw IllegalArgumentException("Método de conexão inválido.")
                    }
                } catch (e: Exception) {
                    showCustomDialog("Atenção", e.message ?: "Erro de validação")
                }
            } else {
                showCustomDialog("Atenção", "Preencha todos os campos corretamente")
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

        // Mostra o drop-down ao clicar no campo de usuário de destino
        binding.etIdUsuarioDestino.setOnClickListener {
            binding.etIdUsuarioDestino.showDropDown()
        }

        // Mostra o drop-down ao clicar no campo de método de conexão
        binding.actvMetodoConexao.setOnClickListener {
            binding.actvMetodoConexao.showDropDown()
        }

        // Mostra o drop-down ao clicar no campo de gateway de pagamento
        binding.actvGatewayPagamento.setOnClickListener {
            binding.actvGatewayPagamento.showDropDown()
        }
    }

    private fun setupMetodoConexaoDropdown() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown_orange,
            metodosConexao
        )
        binding.actvMetodoConexao.setAdapter(adapter)
        binding.actvMetodoConexao.text = null
        binding.actvGatewayPagamento.text = null
    }

    private fun setupGatewayDropdown(metodo: String) {
        val gateways = if (metodo == "INTERNET") gatewaysInternet else gatewaysOffline
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown_orange,
            gateways
        )
        binding.actvGatewayPagamento.setAdapter(adapter)
        binding.actvGatewayPagamento.text = null
    }

    private fun getSelectedUserId(): Long? {
        val selected = binding.etIdUsuarioDestino.text.toString()
        val regex = Regex("""ID:\s*(\d+)""")
        val match = regex.find(selected)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun getUserIdFromToken(): Long? {
        val token = SharedPreferencesHelper(this).getToken() ?: return null
        return try {
            val payload = Base64.decode(token.split(".")[1], Base64.DEFAULT)
            val json = JSONObject(String(payload))
            json.getLong("id")
        } catch (e: Exception) {
            null
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
                showCustomDialog(
                    "Transação realizada!",
                    "ID: ${apiResponse.id}\nValor: R$ %.2f\nMétodo: ${apiResponse.metodoConexao}\nSincronizada: ${if (apiResponse.sincronizada) "Sim" else "Não"}"
                        .format(apiResponse.valor)
                )
                mostrarUltimaTransacao(idUsuarioDestino)
            } catch (e: Exception) {
                showCustomDialog("Erro", "Erro ao realizar transação: ${e.message}")
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
                    showCustomDialog(
                        "Última Transação",
                        "Valor: R$ %.2f\nMétodo: %s\nSincronizada: %s"
                            .format(it.valor, it.metodoConexao, if (it.sincronizada) "Sim" else "Não")
                    )
                }
            } catch (_: Exception) {}
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

data class Usuario(val id: Long, val email: String)