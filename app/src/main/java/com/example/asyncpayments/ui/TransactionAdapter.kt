package com.example.asyncpayments.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ItemTransactionBinding
import com.example.asyncpayments.model.TransactionResponse
import com.example.asyncpayments.utils.AppLogger

class TransactionAdapter(
    private val transactions: List<TransactionResponse>,
    private val tipoConta: String?,
    private val userEmail: String? = null,
    private val userCpf: String? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: TransactionResponse) {
            val context = binding.root.context
            val isMesmoUsuario = transaction.emailUsuarioOrigem == userEmail && transaction.emailUsuarioDestino == userEmail
            val isEntreUsuarios = transaction.emailUsuarioOrigem != null && transaction.emailUsuarioDestino != null &&
                    transaction.emailUsuarioOrigem != transaction.emailUsuarioDestino

            var setaRes = R.drawable.ic_arrow_upward
            var corRes = android.R.color.holo_red_dark

            if (isEntreUsuarios) {
                // Transferência entre usuários
                if (transaction.emailUsuarioOrigem == userEmail) {
                    // Saída (remetente)
                    setaRes = R.drawable.ic_arrow_upward
                    corRes = android.R.color.holo_red_dark
                } else if (transaction.emailUsuarioDestino == userEmail) {
                    // Entrada (destinatário)
                    setaRes = R.drawable.ic_arrow_downward
                    corRes = android.R.color.holo_green_dark
                }
            } else if (isMesmoUsuario) {
                if (tipoConta == "SINCRONA") {
                    when (transaction.tipoOperacao) {
                        "SINCRONIZACAO" -> {
                            // Sincronização: entrada na conta sincrona (seta pra baixo)
                            setaRes = R.drawable.ic_arrow_downward
                            corRes = R.color.orange_primary
                        }
                        "INTERNA" -> {
                            // Adição de fundos: saída da conta sincrona (seta pra cima)
                            setaRes = R.drawable.ic_arrow_upward
                            corRes = R.color.orange_primary
                        }
                        else -> {
                            setaRes = R.drawable.ic_arrow_downward
                            corRes = R.color.orange_primary
                        }
                    }
                } else if (tipoConta == "ASSINCRONA") {
                    when (transaction.tipoOperacao) {
                        "SINCRONIZACAO" -> {
                            // Sincronização: saída da conta assincrona (seta pra cima)
                            setaRes = R.drawable.ic_arrow_upward
                            corRes = R.color.orange_primary
                        }
                        "INTERNA" -> {
                            // Adição de fundos: entrada na conta assincrona (seta pra baixo)
                            setaRes = R.drawable.ic_arrow_downward
                            corRes = R.color.orange_primary
                        }
                        else -> {
                            setaRes = R.drawable.ic_arrow_downward
                            corRes = R.color.orange_primary
                        }
                    }
                }
            }

            binding.ivArrow.setImageResource(setaRes)
            binding.ivArrow.setColorFilter(ContextCompat.getColor(context, corRes))

            binding.tvTransactionMain.text =
                "${transaction.nomeUsuarioOrigem ?: "Usuário"} → ${transaction.nomeUsuarioDestino ?: "Usuário"}"
            binding.tvTransactionValue.text = "R$ %.2f".format(transaction.valor)
            binding.tvTransactionStatus.text = transaction.status
            binding.tvTransactionDate.text = transaction.dataCriacao.let {
                try {
                    val dateTime = it.replace("Z", "").replace("T", " ")
                    if (dateTime.length >= 16) dateTime.substring(0, 16) else dateTime
                } catch (e: Exception) {
                    it
                }
            } ?: "--"

            binding.root.setOnClickListener {
                AppLogger.log("TransactionAdapter", "Abrindo detalhes da transação: $transaction")
                val intent = android.content.Intent(context, TransactionDetailActivity::class.java)
                intent.putExtra("transaction", transaction)
                context.startActivity(intent)
            }

            binding.tvTransactionStatus.text = when (transaction.status) {
                "PENDENTE" -> "Pendente"
                "SINCRONIZADA" -> "Sincronizada"
                "ROLLBACK" -> "Revertida"
                else -> transaction.status
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transacao = transactions[position]
        holder.bind(transacao)
    }

    override fun getItemCount(): Int = transactions.size
}