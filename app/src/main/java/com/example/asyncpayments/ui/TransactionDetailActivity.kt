package com.example.asyncpayments.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.asyncpayments.databinding.ActivityTransactionDetailBinding
import com.example.asyncpayments.model.TransactionResponse

class TransactionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val transaction = intent.getSerializableExtra("transaction") as? TransactionResponse

        if (transaction != null) {
            binding.tvDetailId.text = transaction.id?.toString() ?: "--"
            binding.tvDetailOrigem.text = transaction.nomeUsuarioOrigem ?: "--"
            binding.tvDetailDestino.text = transaction.nomeUsuarioDestino ?: "--"
            binding.tvDetailValor.text = "R$ %.2f".format(transaction.valor)
            binding.tvDetailStatus.text = transaction.status ?: "--"
            binding.tvDetailDescricao.text = transaction.descricao ?: "--"
            binding.tvDetailDataCriacao.text = transaction.dataCriacao ?: "--"
            binding.tvDetailDataAtualizacao.text = transaction.dataAtualizacao ?: "--"
            binding.tvDetailTipoOperacao.text = transaction.tipoOperacao ?: "--"
            binding.tvDetailMetodoConexao.text = transaction.metodoConexao ?: "--"
            binding.tvDetailGateway.text = transaction.gatewayPagamento ?: "--"
        } else {
            finish()
        }

        binding.btnFechar.setOnClickListener { finish() }
    }
}