package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityTransactionBinding
import com.example.asyncpayments.domain.TransactionUseCase
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.UserResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.UserCache
import com.example.asyncpayments.utils.isOnline
import kotlinx.coroutines.launch
import org.json.JSONObject

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private var userIdOrigem: Long? = null
    private var usuarios: List<UserResponse>? = null

    
    private val transactionUseCase by lazy {
        TransactionUseCase(
            RetrofitClient.getInstance(this).create(TransactionService::class.java),
            OfflineTransactionQueue
        )
    }

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

        carregarUsuarios()

        
        binding.etIdUsuarioDestino.setOnItemClickListener { _, _, _, _ ->
            setupMetodoConexaoDropdown()
        }

        
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
            val descricao = binding.etDescricao.text.toString().take(140) 

            if (valor != null && userIdOrigem != null && idUsuarioDestino != null &&
                metodoConexao.isNotBlank() && gatewayPagamento.isNotBlank()) {

                try {
                    if (metodoConexao == "INTERNET") {
                        if (gatewayPagamento !in gatewaysInternet) {
                            throw IllegalArgumentException("Para transações via INTERNET, só são permitidos os gateways: STRIPE, PAGARME, MERCADO_PAGO e INTERNO.")
                        }
                        val request = TransactionRequest(
                            idUsuarioOrigem = userIdOrigem!!,
                            idUsuarioDestino = idUsuarioDestino,
                            valor = valor,
                            metodoConexao = metodoConexao,
                            gatewayPagamento = gatewayPagamento,
                            descricao = descricao 
                        )
                        lifecycleScope.launch {
                            try {
                                val apiResponse = transactionUseCase.sendTransactionOnline(request)
                                ShowNotification.show(
                                    this@TransactionActivity,
                                    ShowNotification.Type.TRANSACTION_SENT,
                                    apiResponse.valor,
                                    "${apiResponse.metodoConexao}\nEnviado para ID: ${apiResponse.idUsuarioDestino}\nValor: R$ %.2f".format(apiResponse.valor) +
                                    (if (!apiResponse.descricao.isNullOrBlank()) "\nDescrição: ${apiResponse.descricao}" else "")
                                )
                            } catch (e: Exception) {
                                ShowNotification.show(
                                    this@TransactionActivity,
                                    ShowNotification.Type.GENERIC,
                                    0.0,
                                    "Erro ao realizar transação: ${e.message}"
                                )
                            }
                        }
                    } else if (metodoConexao in listOf("SMS", "NFC", "BLUETOOTH")) {
                        if (gatewayPagamento !in gatewaysOffline) {
                            throw IllegalArgumentException("Para transações offline (SMS, NFC, BLUETOOTH), só são permitidos os gateways: DREX, PAGSEGURO e PAYCERTIFY.")
                        }
                        val token = SharedPreferencesHelper(this).getToken()
                        val emailOrigem = token?.let {
                            try {
                                val payload = Base64.decode(it.split(".")[1], Base64.DEFAULT)
                                val json = JSONObject(String(payload))
                                json.getString("sub")
                            } catch (e: Exception) {
                                ""
                            }
                        } ?: ""
                        val emailDestino = usuarios?.find { it.id == idUsuarioDestino }?.email ?: ""
                        val paymentData = PaymentData(
                            id = System.currentTimeMillis(),
                            valor = valor,
                            origem = emailOrigem,
                            destino = emailDestino,
                            data = System.currentTimeMillis().toString(),
                            metodoConexao = metodoConexao,
                            gatewayPagamento = gatewayPagamento,
                            descricao = descricao 
                        )
                        transactionUseCase.saveTransactionOffline(this, paymentData)
                        ShowNotification.show(
                            this,
                            ShowNotification.Type.TRANSACTION_SENT,
                            valor,
                            "Transação offline salva com sucesso!"
                        )
                    } else {
                        throw IllegalArgumentException("Método de conexão inválido.")
                    }
                } catch (e: Exception) {
                    ShowNotification.show(
                        this,
                        ShowNotification.Type.GENERIC,
                        0.0,
                        e.message ?: "Erro de validação"
                    )
                }
            } else {
                ShowNotification.show(
                    this,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Preencha todos os campos corretamente"
                )
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

        
        binding.etIdUsuarioDestino.setOnClickListener {
            binding.etIdUsuarioDestino.showDropDown()
        }

        
        binding.actvMetodoConexao.setOnClickListener {
            binding.actvMetodoConexao.showDropDown()
        }

        
        binding.actvGatewayPagamento.setOnClickListener {
            binding.actvGatewayPagamento.showDropDown()
        }
    }

    private fun carregarUsuarios() {
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            val isOnline = isOnline(this@TransactionActivity)
            usuarios = if (isOnline) {
                try {
                    val lista = userService.listarUsuarios()
                    UserCache.save(this@TransactionActivity, lista)
                    lista
                } catch (e: Exception) {
                    UserCache.load(this@TransactionActivity)
                }
            } else {
                UserCache.load(this@TransactionActivity)
            }
            val userList = usuarios?.map { "${it.email} (ID: ${it.id})" } ?: emptyList()
            val adapter = ArrayAdapter(
                this@TransactionActivity,
                R.layout.item_dropdown_orange,
                userList
            )
            binding.etIdUsuarioDestino.setAdapter(adapter)
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
}