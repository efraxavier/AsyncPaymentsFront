package com.example.asyncpayments.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.asyncpayments.databinding.ActivityRegisterBinding
import com.example.asyncpayments.model.RegisterRequest
import com.example.asyncpayments.network.AuthService
import com.example.asyncpayments.network.RetrofitClient
import com.example.asyncpayments.utils.ShowNotification
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

    private val steps = listOf(
        "Insira seu e-mail",
        "Insira sua senha",
        "Insira seu nome",
        "Insira seu sobrenome",
        "Insira seu CPF",
        "Insira seu celular",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showStep()

        binding.btnNext.setOnClickListener {
            when (step) {
                0 -> {
                    email = binding.etRegisterInput.text.toString()
                    if (email.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha o e-mail")
                        return@setOnClickListener
                    }
                }
                1 -> {
                    password = binding.etRegisterInput.text.toString()
                    if (password.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha a senha")
                        return@setOnClickListener
                    }
                }
                2 -> {
                    nome = binding.etRegisterInput.text.toString()
                    if (nome.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha o nome")
                        return@setOnClickListener
                    }
                }
                3 -> {
                    sobrenome = binding.etRegisterInput.text.toString()
                    if (sobrenome.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha o sobrenome")
                        return@setOnClickListener
                    }
                }
                4 -> {
                    cpf = binding.etRegisterInput.text.toString()
                    if (cpf.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha o CPF")
                        return@setOnClickListener
                    }
                }
                5 -> {
                    celular = binding.etRegisterInput.text.toString()
                    if (celular.isBlank()) {
                        ShowNotification.show(this, ShowNotification.Type.REGISTER_ERROR, 0.0, "Preencha o celular")
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
            role = "USER"
        )

        lifecycleScope.launch {
            try {
                authService.registrar(registerRequest)
                exibirDialogoLGPD()
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("409") == true || e.message?.contains("CONFLICT") == true -> {
                        "E-mail já cadastrado."
                    }
                    e.message?.contains("401") == true || e.message?.contains("UNAUTHORIZED") == true -> {
                        "Usuário ou senha inválidos."
                    }
                    else -> {
                        e.message ?: "Erro desconhecido"
                    }
                }
                ShowNotification.show(
                    this@RegisterActivity,
                    ShowNotification.Type.GENERIC,
                    0.0,
                    msg
                )
            }
        }
    }

    private fun exibirDialogoLGPD() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Termos de Privacidade - LGPD")
            .setMessage(
                "De acordo com a Lei Geral de Proteção de Dados (LGPD - Lei nº 13.709/2018), " +
                "é necessário o seu consentimento para o uso dos dados fornecidos. " +
                "Os dados serão utilizados exclusivamente para fins de cadastro e transações financeiras. " +
                "Você pode revogar este consentimento a qualquer momento, conforme o Artigo 9º da LGPD. " +
                "Deseja continuar?"
            )
            .setPositiveButton("Concordo") { _, _ ->
                showSuccessAndLogin()
            }
            .setNegativeButton("Não concordo") { _, _ ->
                ShowNotification.show(
                    this,
                    ShowNotification.Type.REGISTER_ERROR,
                    0.0,
                    "Cadastro não realizado. Você deve concordar com os termos para prosseguir."
                )
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun showSuccessAndLogin() {
        ShowNotification.show(
            this,
            ShowNotification.Type.REGISTER_SUCCESS,
            0.0,
            "Cadastro realizado com sucesso! Você será redirecionado."
        )
        
        binding.root.postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("password", password)
            startActivity(intent)
            finish()
        }, 2000)
    }
}