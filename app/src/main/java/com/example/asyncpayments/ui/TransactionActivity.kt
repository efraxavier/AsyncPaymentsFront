package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.example.asyncpayments.utils.isOnline
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private var userIdOrigem: Long? = null
    private var usuarios: List<UserResponse>? = null

    private val metodosConexao = listOf("INTERNET", "BLUETOOTH", "SMS", "NFC")
    private val gatewaysInternet = listOf("STRIPE", "PAGARME", "MERCADO_PAGO")
    private val gatewaysOffline = listOf("DREX", "PAGSEGURO", "PAYCERTIFY")

    private var step = 0
    private var idUsuarioDestino: Long? = null
    private var valor: Double? = null
    private var metodoConexao: String? = null
    private var gatewayPagamento: String? = null
    private var descricao: String = ""

    private val steps = listOf(
        "Selecione o destinatário",
        "Informe o valor",
        "Escolha o método de conexão",
        "Escolha o gateway de pagamento",
        "Descrição (opcional)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userIdOrigem = getUserIdFromToken()
        carregarUsuarios()

        setupStepUI()

        binding.btnNext.setOnClickListener {
            if (!validateStep()) return@setOnClickListener
            step++
            if (step < steps.size) {
                setupStepUI()
            } else {
                showConfirmDialog()
            }
        }

        binding.btnBack.setOnClickListener {
            if (step > 0) {
                step--
                setupStepUI()
            }
        }

        binding.etGenericInput.setOnClickListener {
            if (step == 0 && binding.etGenericInput is AutoCompleteTextView) {
                (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
            }
            if (step == 2 && binding.etGenericInput is AutoCompleteTextView) {
                (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
            }
            if (step == 3 && binding.etGenericInput is AutoCompleteTextView) {
                (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
            }
        }

        binding.bottomNav.menu.findItem(R.id.menu_transactions).isChecked = true
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_add_funds -> {
                    startActivity(Intent(this, AddFundsActivity::class.java))
                    finish()
                    true
                }
                R.id.menu_transactions -> true
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupStepUI() {
        binding.tilGenericInput.visibility = View.VISIBLE
        binding.etGenericInput.setText("")
        binding.etGenericInput.hint = steps[step]
        binding.btnBack.visibility = if (step == 0) View.INVISIBLE else View.VISIBLE
        binding.btnNext.text = if (step == steps.lastIndex) "Finalizar" else "Próximo"

        (binding.etGenericInput as? AutoCompleteTextView)?.setAdapter(null)
        binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        binding.etGenericInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
        binding.etGenericInput.isFocusable = true
        binding.etGenericInput.isFocusableInTouchMode = true
        binding.etGenericInput.isEnabled = true
        binding.etGenericInput.setOnClickListener(null)

        when (step) {
            0 -> {
                val userList = usuarios?.map { "${it.email} (ID: ${it.id})" } ?: emptyList()
                Log.d("TransactionActivity", "Configuração do dropdown: $userList")
                val adapter = ArrayAdapter(this, R.layout.item_dropdown_orange, userList)
                (binding.etGenericInput as? AutoCompleteTextView)?.setAdapter(adapter)
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
                binding.etGenericInput.inputType = android.text.InputType.TYPE_NULL
                binding.etGenericInput.isFocusable = false
                binding.etGenericInput.isFocusableInTouchMode = false
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setOnClickListener {
                    (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
                }
            }
            1 -> {
                binding.etGenericInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                binding.etGenericInput.isFocusable = true
                binding.etGenericInput.isFocusableInTouchMode = true
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
            2 -> {
                val adapter = ArrayAdapter(this, R.layout.item_dropdown_orange, metodosConexao)
                (binding.etGenericInput as? AutoCompleteTextView)?.setAdapter(adapter)
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
                binding.etGenericInput.inputType = android.text.InputType.TYPE_NULL
                binding.etGenericInput.isFocusable = false
                binding.etGenericInput.isFocusableInTouchMode = false
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setOnClickListener {
                    (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
                }
            }
            3 -> {
                val gateways = if (metodoConexao == "INTERNET") gatewaysInternet else gatewaysOffline
                val adapter = ArrayAdapter(this, R.layout.item_dropdown_orange, gateways)
                (binding.etGenericInput as? AutoCompleteTextView)?.setAdapter(adapter)
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
                binding.etGenericInput.inputType = android.text.InputType.TYPE_NULL
                binding.etGenericInput.isFocusable = false
                binding.etGenericInput.isFocusableInTouchMode = false
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setOnClickListener {
                    (binding.etGenericInput as? AutoCompleteTextView)?.showDropDown()
                }
            }
            4 -> {
                binding.etGenericInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                binding.etGenericInput.isFocusable = true
                binding.etGenericInput.isFocusableInTouchMode = true
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }

    private fun validateStep(): Boolean {
        val input = binding.etGenericInput.text.toString()
        when (step) {
            0 -> {
                val regex = Regex("""ID:\s*(\d+)""")
                val match = regex.find(input)
                idUsuarioDestino = match?.groupValues?.get(1)?.toLongOrNull()
                if (idUsuarioDestino == null) {
                    if (!isFinishing && !isDestroyed) {
                        ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Selecione um destinatário válido")
                    }
                    return false
                }
            }
            1 -> {
                valor = input.toDoubleOrNull()
                if (valor == null || valor!! <= 0.0) {
                    if (!isFinishing && !isDestroyed) {
                        ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Informe um valor válido")
                    }
                    return false
                }
            }
            2 -> {
                metodoConexao = input
                if (metodoConexao.isNullOrBlank() || metodoConexao !in metodosConexao) {
                    if (!isFinishing && !isDestroyed) {
                        ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Escolha um método de conexão")
                    }
                    return false
                }
            }
            3 -> {
                gatewayPagamento = input
                val gateways = if (metodoConexao == "INTERNET") gatewaysInternet else gatewaysOffline
                if (gatewayPagamento.isNullOrBlank() || gatewayPagamento !in gateways) {
                    if (!isFinishing && !isDestroyed) {
                        ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Escolha um gateway válido")
                    }
                    return false
                }
            }
            4 -> {
                descricao = input.take(140)
            }
        }
        return true
    }

    private fun showConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar transação?")
            .setMessage(
                "Destinatário: $idUsuarioDestino\n" +
                "Valor: R$ %.2f\n".format(valor ?: 0.0) +
                "Método: $metodoConexao\n" +
                "Gateway: $gatewayPagamento\n" +
                if (descricao.isNotBlank()) "Descrição: $descricao" else ""
            )
            .setPositiveButton("Sim") { _, _ -> enviarTransacao() }
            .setNegativeButton("Não") { _, _ -> }
            .show()
    }

    private fun enviarTransacao() {
        if (userIdOrigem == null || idUsuarioDestino == null || valor == null || metodoConexao.isNullOrBlank() || gatewayPagamento.isNullOrBlank()) {
            ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Preencha todos os campos obrigatórios")
            return
        }
        val request = TransactionRequest(
            idUsuarioOrigem = userIdOrigem!!,
            idUsuarioDestino = idUsuarioDestino!!,
            valor = valor!!,
            metodoConexao = metodoConexao!!,
            gatewayPagamento = gatewayPagamento!!,
            descricao = descricao
        )
        val transactionUseCase = TransactionUseCase(
            RetrofitClient.getInstance(this).create(TransactionService::class.java),
            OfflineTransactionQueue
        )
        lifecycleScope.launch {
            try {
                if (metodoConexao == "INTERNET") {
                    val apiResponse = transactionUseCase.sendTransactionOnline(request)
                    ShowNotification.show(
                        this@TransactionActivity,
                        ShowNotification.Type.TRANSACTION_SENT,
                        apiResponse.valor,
                        "Transação realizada com sucesso!"
                    )
                } else {
                    val paymentData = PaymentData(
                        id = System.currentTimeMillis(),
                        valor = valor!!,
                        origem = "emailOrigem",
                        destino = "emailDestino",
                        data = System.currentTimeMillis().toString(),
                        metodoConexao = metodoConexao!!,
                        gatewayPagamento = gatewayPagamento!!,
                        descricao = descricao
                    )
                    transactionUseCase.saveTransactionOffline(this@TransactionActivity, paymentData)
                    ShowNotification.show(
                        this@TransactionActivity,
                        ShowNotification.Type.TRANSACTION_SENT,
                        valor!!,
                        "Transação offline salva com sucesso!"
                    )
                }
            } catch (e: Exception) {
                ShowNotification.show(
                    this@TransactionActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    "Erro ao realizar transação: ${e.message}"
                )
            }
        }
    }

    private fun carregarUsuarios() {
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        lifecycleScope.launch {
            val isOnline = isOnline(this@TransactionActivity)
            usuarios = if (isOnline) {
                try {
                    val listaUsuarios = userService.listarUsuarios()
                    Log.d("TransactionActivity", "Usuários carregados: $listaUsuarios")
                    listaUsuarios
                } catch (e: Exception) {
                    Log.e("TransactionActivity", "Erro ao carregar usuários: ${e.message}")
                    emptyList()
                }
            } else {
                Log.d("TransactionActivity", "Offline: lista de usuários vazia.")
                emptyList()
            }

            val userIdOrigem = getUserIdFromToken()
            usuarios = usuarios?.filter { it.id != userIdOrigem }
            Log.d("TransactionActivity", "Usuários filtrados: $usuarios")

            if (step == 0) setupStepUI()
        }
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