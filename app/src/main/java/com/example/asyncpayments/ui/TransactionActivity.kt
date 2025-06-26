package com.example.asyncpayments.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.comms.SMSSender
import com.example.asyncpayments.databinding.ActivityTransactionBinding
import com.example.asyncpayments.model.PaymentData
import com.example.asyncpayments.model.TransactionRequest
import com.example.asyncpayments.model.UserResponse
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.network.TransactionService
import com.example.asyncpayments.network.UserService
import com.example.asyncpayments.utils.AppLogger
import com.example.asyncpayments.utils.OfflineModeManager
import com.example.asyncpayments.utils.OfflineTransactionQueue
import com.example.asyncpayments.utils.SharedPreferencesHelper
import com.example.asyncpayments.utils.ShowNotification
import com.example.asyncpayments.utils.TokenUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

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

    private lateinit var transactionService: TransactionService

    companion object {
        private const val REQUEST_SMS_PERMISSION = 1001
    }

    private var pendingPaymentData: PaymentData? = null
    private var pendingPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLogger.log("TransactionActivity", "onCreate chamado")
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialize o OfflineTransactionQueue
        val db = com.example.asyncpayments.data.AppDatabase.getInstance(this)
        com.example.asyncpayments.utils.OfflineTransactionQueue.init(db.offlineTransactionDao())

        transactionService = RetrofitClient.getInstance(this).create(TransactionService::class.java)

        userIdOrigem = TokenUtils.getUserIdFromToken(this)
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

    override fun onStart() {
        AppLogger.log("TransactionActivity", "onStart chamado")
        super.onStart()
    }

    override fun onResume() {
        AppLogger.log("TransactionActivity", "onResume chamado")
        super.onResume()
    }

    override fun onPause() {
        AppLogger.log("TransactionActivity", "onPause chamado")
        super.onPause()
    }

    override fun onStop() {
        AppLogger.log("TransactionActivity", "onStop chamado")
        super.onStop()
    }

    override fun onDestroy() {
        AppLogger.log("TransactionActivity", "onDestroy chamado")
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        AppLogger.log("TransactionActivity", "onSaveInstanceState chamado")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        AppLogger.log("TransactionActivity", "onRestoreInstanceState chamado")
        super.onRestoreInstanceState(savedInstanceState)
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
                if (idUsuarioDestino != null) {
                    val user = usuarios?.find { it.id == idUsuarioDestino }
                    if (user != null) {
                        binding.etGenericInput.setText("${user.email} (ID: ${user.id})", false)
                    }
                }
            }
            1 -> {
                binding.etGenericInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                binding.etGenericInput.isFocusable = true
                binding.etGenericInput.isFocusableInTouchMode = true
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                if (valor != null) {
                    binding.etGenericInput.setText(valor.toString())
                }
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
                if (!metodoConexao.isNullOrBlank()) {
                    binding.etGenericInput.setText(metodoConexao, false)
                }
                val input = binding.etGenericInput.text.toString()
                if (input == "SMS" || metodoConexao == "SMS") {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
                    }
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
                if (!gatewayPagamento.isNullOrBlank()) {
                    binding.etGenericInput.setText(gatewayPagamento, false)
                }
            }
            4 -> {
                binding.etGenericInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                binding.etGenericInput.isFocusable = true
                binding.etGenericInput.isFocusableInTouchMode = true
                binding.etGenericInput.isEnabled = true
                binding.etGenericInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                if (descricao.isNotBlank()) {
                    binding.etGenericInput.setText(descricao)
                }
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
                if (metodoConexao == "SMS") {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
                    }
                }
            }
            3 -> {
                gatewayPagamento = input
                AppLogger.log("TransactionActivity", "Gateway selecionado: $gatewayPagamento")
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
        if (isFinishing || isDestroyed) return
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
        val origemEmail = TokenUtils.getEmailFromToken(this)
        if (origemEmail.isNullOrBlank()) {
            AppLogger.log("TransactionActivity", "Erro: origem do usuário está vazia ou nula!")
            ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Erro: usuário de origem não encontrado.")
            return
        }
        val destinoEmail = usuarios?.find { it.id == idUsuarioDestino }?.email
        if (destinoEmail.isNullOrBlank()) {
            AppLogger.log("TransactionActivity", "Erro: destino do usuário está vazio ou nulo!")
            ShowNotification.show(this, ShowNotification.Type.GENERIC, 0.0, "Erro: usuário de destino não encontrado.")
            return
        }

        val identificadorOffline = UUID.randomUUID().toString()
        val paymentData = PaymentData(
            id = System.currentTimeMillis(),
            valor = valor!!,
            origem = origemEmail,
            destino = destinoEmail,
            data = System.currentTimeMillis().toString(),
            metodoConexao = metodoConexao!!,
            gatewayPagamento = gatewayPagamento!!,
            descricao = descricao,
            dataCriacao = System.currentTimeMillis(),
            identificadorOffline = identificadorOffline
        )
        AppLogger.log("TransactionActivity", "Geração de PaymentData para transação offline: $paymentData")

        if (metodoConexao == "INTERNET") {
            lifecycleScope.launch {
                try {
                    val usuarioOrigem = usuarios?.find { it.id == userIdOrigem }
                    val usuarioDestino = usuarios?.find { it.id == idUsuarioDestino }

                    val tipoOperacao = if (metodoConexao == "INTERNET") "SINCRONA" else "ASSINCRONA"

                    val request = TransactionRequest(
                        idUsuarioOrigem = userIdOrigem!!,
                        idUsuarioDestino = idUsuarioDestino!!,
                        valor = valor!!,
                        tipoOperacao = tipoOperacao,
                        metodoConexao = metodoConexao!!,
                        gatewayPagamento = gatewayPagamento!!,
                        descricao = descricao,
                        nomeUsuarioOrigem = usuarioOrigem?.nome,
                        emailUsuarioOrigem = usuarioOrigem?.email,
                        cpfUsuarioOrigem = usuarioOrigem?.cpf,
                        nomeUsuarioDestino = usuarioDestino?.nome,
                        emailUsuarioDestino = usuarioDestino?.email,
                        cpfUsuarioDestino = usuarioDestino?.cpf
                    )
                    AppLogger.log("TransactionActivity", "Enviando transação online: $request")
                    val response = transactionService.sendTransaction(request)
                    AppLogger.log("TransactionActivity", "Transação enviada pela internet: $response")
                    Toast.makeText(
                        applicationContext,
                        "Transação enviada com sucesso pela internet!",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@TransactionActivity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                } catch (e: Exception) {
                    AppLogger.log("TransactionActivity", "Erro ao enviar transação online: ${e.message}", e)
                    // Se falhar, salva offline
                    val saldoAtual = SharedPreferencesHelper(this@TransactionActivity).getSaldoLocal() ?: 0.0

                    OfflineTransactionQueue.saveTransaction(
                        this@TransactionActivity,
                        paymentData,
                        userIdOrigem!!,        
                        idUsuarioDestino!!,    
                        saldoAtual             
                    )
                    AppLogger.log("TransactionActivity", "Transação salva offline após falha online: $paymentData")
                    Toast.makeText(
                        applicationContext,
                        "Falha ao enviar online. Transação salva offline.",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(this@TransactionActivity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                }
            }
        } else {
            val saldoAtual = SharedPreferencesHelper(this).getSaldoLocal() ?: 0.0

            OfflineTransactionQueue.saveTransaction(
                this,
                paymentData,
                userIdOrigem!!,
                idUsuarioDestino!!,
                saldoAtual
            )
            AppLogger.log("TransactionActivity", "Transação registrada localmente: $paymentData")

            AppLogger.log("TransactionActivity", "Antes do envio de SMS: $paymentData")
            if (metodoConexao == "SMS") {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    AppLogger.log("TransactionActivity", "Enviando SMS para outro emulador com dados: $paymentData")
                    com.example.asyncpayments.comms.SMSSender(this).send(paymentData)
                    AppLogger.log("TransactionActivity", "Depois do envio de SMS: $paymentData")
                    Toast.makeText(this, "SMS enviado!", Toast.LENGTH_SHORT).show()
                    // Aguarda 1 segundo antes de fechar (apenas para garantir emulador)
                    binding.root.postDelayed({
                        startActivity(Intent(this, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        finish()
                    }, 1000)
                    return
                } else {
                    AppLogger.log("TransactionActivity", "Permissão de SMS não concedida no momento do envio.")
                }
            }

            Toast.makeText(
                applicationContext,
                "Transação registrada localmente. Será enviada ao servidor assim que houver conexão.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        AppLogger.log("TransactionActivity", "onRequestPermissionsResult chamado. requestCode=$requestCode, permissions=${permissions.joinToString()}, grantResults=${grantResults.joinToString()}")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.log("TransactionActivity", "Permissão de SMS concedida no callback de permissão.")
                val paymentData = pendingPaymentData
                val phoneNumber = pendingPhoneNumber
                if (paymentData != null && phoneNumber != null) {
                    AppLogger.log("TransactionActivity", "Enviando SMS após permissão concedida. paymentData=$paymentData, phoneNumber=$phoneNumber")
                    SMSSender(this).send(paymentData, phoneNumber)
                }
            } else {
                AppLogger.log("TransactionActivity", "Permissão de SMS negada no callback de permissão.")
            }
            pendingPaymentData = null
            pendingPhoneNumber = null
            AppLogger.log("TransactionActivity", "Limpeza de dados pendentes após callback de permissão.")
        }
    }


    private fun carregarUsuarios() {
        val userService = RetrofitClient.getInstance(this).create(UserService::class.java)
        val prefs = getSharedPreferences("user_cache", MODE_PRIVATE)
        val gson = Gson()
        lifecycleScope.launch {
            try {
                val isOffline = OfflineModeManager.isOffline
                usuarios = if (!isOffline) {
                    try {
                        val listaUsuarios = userService.listarUsuarios()
                        prefs.edit().putString("usuarios", gson.toJson(listaUsuarios)).apply()
                        listaUsuarios
                    } catch (e: Exception) {
                        val json = prefs.getString("usuarios", null)
                        if (json != null) {
                            val type = object : com.google.gson.reflect.TypeToken<List<UserResponse>>() {}.type
                            gson.fromJson<List<UserResponse>>(json, type)
                        } else {
                            emptyList()
                        }
                    }
                } else {
                    val json = prefs.getString("usuarios", null)
                    if (json != null) {
                        val type = object : com.google.gson.reflect.TypeToken<List<UserResponse>>() {}.type
                        gson.fromJson<List<UserResponse>>(json, type)
                    } else {
                        emptyList()
                    }
                }

                val userIdOrigem = TokenUtils.getUserIdFromToken(this@TransactionActivity)
                usuarios = usuarios?.filter { it.id != userIdOrigem }

                if (usuarios.isNullOrEmpty()) {
                    if (!isFinishing && !isDestroyed) {
                        ShowNotification.show(
                            this@TransactionActivity,
                            ShowNotification.Type.GENERIC,
                            0.0,
                            "Nenhum usuário disponível offline. Conecte-se à internet para atualizar a lista."
                        )
                    }
                }

                if (step == 0) setupStepUI()
            } catch (e: Exception) {
                AppLogger.log("TransactionActivity", "Erro em carregarUsuarios: ${e.message}", e)
            }
        }
    }
}