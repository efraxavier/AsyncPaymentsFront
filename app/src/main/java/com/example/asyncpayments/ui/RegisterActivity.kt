package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ActivityRegisterBinding
import com.example.asyncpayments.databinding.DialogResponseBinding
import com.example.asyncpayments.model.RegisterRequest
import com.example.asyncpayments.network.AuthService
import com.example.asyncpayments.network.RetrofitClient
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private var step = 0
    private var email = ""
    private var password = ""
    private var nome = ""
    private var sobrenome = ""
    private var cpf = ""
    private var celular = ""
    private var formaPagamento = ""

    private val steps = listOf(
        "Insira seu e-mail",
        "Insira sua senha",
        "Insira seu nome",
        "Insira seu sobrenome",
        "Insira seu CPF",
        "Insira seu celular",
        "Escolha a forma de pagamento"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val formasPagamento = listOf("CREDITO", "DEBITO")
        val pagamentoAdapter = ArrayAdapter(this, R.layout.item_dropdown_orange, formasPagamento)
        binding.actvFormaPagamento.setAdapter(pagamentoAdapter)
        binding.actvFormaPagamento.setOnClickListener { binding.actvFormaPagamento.showDropDown() }

        showStep()

        binding.btnNext.setOnClickListener {
            when (step) {
                0 -> {
                    email = binding.etRegisterInput.text.toString()
                    if (email.isBlank()) {
                        showCustomDialog("Atenção", "Preencha o e-mail")
                        return@setOnClickListener
                    }
                }
                1 -> {
                    password = binding.etRegisterInput.text.toString()
                    if (password.isBlank()) {
                        showCustomDialog("Atenção", "Preencha a senha")
                        return@setOnClickListener
                    }
                }
                2 -> {
                    nome = binding.etRegisterInput.text.toString()
                    if (nome.isBlank()) {
                        showCustomDialog("Atenção", "Preencha o nome")
                        return@setOnClickListener
                    }
                }
                3 -> {
                    sobrenome = binding.etRegisterInput.text.toString()
                    if (sobrenome.isBlank()) {
                        showCustomDialog("Atenção", "Preencha o sobrenome")
                        return@setOnClickListener
                    }
                }
                4 -> {
                    cpf = binding.etRegisterInput.text.toString()
                    if (cpf.isBlank()) {
                        showCustomDialog("Atenção", "Preencha o CPF")
                        return@setOnClickListener
                    }
                }
                5 -> {
                    celular = binding.etRegisterInput.text.toString()
                    if (celular.isBlank()) {
                        showCustomDialog("Atenção", "Preencha o celular")
                        return@setOnClickListener
                    }
                }
                6 -> {
                    formaPagamento = binding.actvFormaPagamento.text.toString()
                    if (formaPagamento.isBlank()) {
                        showCustomDialog("Atenção", "Escolha a forma de pagamento")
                        return@setOnClickListener
                    }
                }
            }
            step++
            if (step < steps.size) {
                showStep()
            } else {
                realizarCadastro()
            }
        }
    }

    private fun showStep() {
        binding.etRegisterInput.setText("")
        binding.etRegisterInput.visibility = android.view.View.VISIBLE
        binding.tilRegisterInput.visibility = android.view.View.VISIBLE
        binding.actvFormaPagamento.visibility = android.view.View.GONE

        when (step) {
            0 -> {
                binding.tvRegisterPrompt.text = steps[0]
                binding.etRegisterInput.hint = "E-mail"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }
            1 -> {
                binding.tvRegisterPrompt.text = steps[1]
                binding.etRegisterInput.hint = "Senha"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            2 -> {
                binding.tvRegisterPrompt.text = steps[2]
                binding.etRegisterInput.hint = "Nome"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            3 -> {
                binding.tvRegisterPrompt.text = steps[3]
                binding.etRegisterInput.hint = "Sobrenome"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
            }
            4 -> {
                binding.tvRegisterPrompt.text = steps[4]
                binding.etRegisterInput.hint = "CPF"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            5 -> {
                binding.tvRegisterPrompt.text = steps[5]
                binding.etRegisterInput.hint = "Celular"
                binding.etRegisterInput.inputType = android.text.InputType.TYPE_CLASS_PHONE
            }
            6 -> {
                binding.tvRegisterPrompt.text = steps[6]
                binding.tilRegisterInput.visibility = android.view.View.GONE
                binding.etRegisterInput.visibility = android.view.View.GONE
                binding.actvFormaPagamento.visibility = android.view.View.VISIBLE
                binding.actvFormaPagamento.setText("")
            }
        }
    }

    private fun realizarCadastro() {
        val retrofit = RetrofitClient.getInstance(this)
        val authService = retrofit.create(AuthService::class.java)
        val registerRequest = RegisterRequest(
            email = email,
            password = password,
            nome = nome,
            sobrenome = sobrenome,
            cpf = cpf,
            celular = celular,
            formaPagamentoAlternativa = formaPagamento,
            role = "USER"
        )

        lifecycleScope.launch {
            try {
                authService.register(registerRequest)
                showSuccessAndLogin()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val userMsg = when {
                    msg.contains("cpf") && msg.contains("já existe", ignoreCase = true) -> "Já existe um cadastro com este CPF."
                    msg.contains("email") && msg.contains("já existe", ignoreCase = true) -> "Já existe um cadastro com este e-mail."
                    else -> "Erro ao cadastrar: ${msg.lineSequence().firstOrNull { it.contains("Erro interno") }?.substringAfter("Erro interno: ") ?: msg}"
                }
                showCustomDialog("Erro", userMsg)
            }
        }
    }

    private fun showSuccessAndLogin() {
        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvDialogTitle.text = "Cadastro realizado"
        dialogBinding.tvDialogMessage.text = "Cadastro realizado com sucesso! Você será redirecionado."
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialog.show()
        dialogBinding.btnDialogOk.setOnClickListener {
            dialog.dismiss()
            // Login automático
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("password", password)
            startActivity(intent)
            finish()
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