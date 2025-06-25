package com.example.asyncpayments.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.example.asyncpayments.utils.AppLogger
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ItemTransactionBinding
import com.example.asyncpayments.model.TransactionResponse
import java.time.OffsetDateTime

class TransactionAdapter(
    private val transactions: List<TransactionResponse>,
    private val tipoConta: String?,
    private val userEmail: String? = null,
    private val userCpf: String? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindEmpty() {
            binding.tvTransactionMain.text = "Nenhuma transação encontrada"
            binding.tvTransactionValue.text = ""
            binding.tvTransactionStatus.text = ""
            binding.tvTransactionDate.text = ""
            binding.root.setBackgroundResource(0)
        }

        fun bind(transaction: TransactionResponse, isExpanded: Boolean) {
            val isSaida = (userEmail != null && transaction.emailUsuarioOrigem == userEmail) ||
                          (userCpf != null && transaction.cpfUsuarioOrigem == userCpf)
            val isEntrada = (userEmail != null && transaction.emailUsuarioDestino == userEmail) ||
                            (userCpf != null && transaction.cpfUsuarioDestino == userCpf)
            val isPropriaConta = isSaida && isEntrada

            val isAddFunds = transaction.tipoOperacao == "INTERNA" &&
                             transaction.metodoConexao == "ASYNC" &&
                             transaction.gatewayPagamento == "INTERNO"

            val isSync = transaction.tipoOperacao == "SINCRONIZACAO" &&
                         transaction.metodoConexao == "INTERNET" &&
                         transaction.gatewayPagamento == "INTERNO"

            binding.ivArrow.visibility = View.VISIBLE
            when {
                isAddFunds && tipoConta == "SINCRONA" -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.orange_primary))
                }
                isAddFunds && tipoConta == "ASSINCRONA" -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.orange_primary))
                }
                isSync && tipoConta == "SINCRONA" -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.orange_primary))
                }
                isSync && tipoConta == "ASSINCRONA" -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.orange_primary))
                }
                isSaida && !isEntrada -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.red_accent))
                }
                isEntrada && !isSaida -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.green_active))
                }
                isPropriaConta -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.green_active))
                }
                else -> {
                    binding.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.light_gray))
                }
            }

            binding.tvTransactionMain.text = "${transaction.nomeUsuarioOrigem} → ${transaction.nomeUsuarioDestino}"
            binding.tvTransactionValue.text = "R$ %.2f".format(transaction.valor)
            binding.tvTransactionStatus.text = transaction.status
            binding.tvTransactionDate.text = transaction.dataCriacao?.let {
                try {
                    val dateTime = it.replace("Z", "").replace("T", " ")
                    if (dateTime.length >= 16) dateTime.substring(0, 16) else dateTime
                } catch (e: Exception) {
                    it
                }
            } ?: "--"

            val isOfflinePending = (transaction.status == "PENDENTE" && transaction.id == null)

            if (isOfflinePending) {
                binding.root.setBackgroundResource(R.drawable.bg_offline_pending)
                binding.tvTransactionStatus.text = "PENDENTE (offline)"
                val horas = calcularHorasRestantes(transaction.dataCriacao)
                binding.tvTransactionDate.text = "Faltam $horas h para sincronizar"
            } else {
                binding.root.setBackgroundResource(0)
                binding.tvTransactionStatus.text = transaction.status
                binding.tvTransactionDate.text = transaction.dataCriacao?.let {
                    try {
                        val dateTime = it.replace("Z", "").replace("T", " ")
                        if (dateTime.length >= 16) dateTime.substring(0, 16) else dateTime
                    } catch (e: Exception) {
                        it
                    }
                } ?: "--"
            }

            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = android.content.Intent(context, TransactionDetailActivity::class.java)
                intent.putExtra("transaction", transaction)
                context.startActivity(intent)
            }

            AppLogger.log(
                "TransactionAdapter",
                "userEmail=$userEmail, userCpf=$userCpf, emailOrigem=${transaction.emailUsuarioOrigem}, emailDestino=${transaction.emailUsuarioDestino}, cpfOrigem=${transaction.cpfUsuarioOrigem}, cpfDestino=${transaction.cpfUsuarioDestino}"
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_empty_transaction, parent, false)
            EmptyViewHolder(view)
        } else {
            val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            TransactionViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (transactions.isEmpty()) {
            (holder as EmptyViewHolder).bind(tipoConta)
            return
        }
        (holder as TransactionViewHolder).bind(transactions[position], expandedPositions.contains(position))
    }

    override fun getItemCount(): Int = if (transactions.isEmpty()) 1 else transactions.size

    override fun getItemViewType(position: Int): Int {
        return if (transactions.isEmpty()) 0 else 1
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(tipoConta: String?) {
            val msg = if (tipoConta == "SINCRONA") {
                "Você ainda não tem transações síncronas."
            } else {
                "Você ainda não tem transações assíncronas."
            }
            itemView.findViewById<TextView>(R.id.tvEmptyMessage).text = msg
        }
    }
}

fun getStatusColor(status: String?, context: Context): Int {
    return when (status) {
        "PENDENTE" -> ContextCompat.getColor(context, R.color.orange_primary)
        "SINCRONIZADA" -> ContextCompat.getColor(context, R.color.green_active)
        "ROLLBACK", "ERRO" -> ContextCompat.getColor(context, R.color.red_accent)
        else -> ContextCompat.getColor(context, R.color.light_gray)
    }
}

fun isOfflinePending(transaction: TransactionResponse): Boolean {
    return transaction.status == "PENDENTE" && transaction.metodoConexao != "INTERNET"
}

private fun calcularHorasRestantes(dataCriacao: String): Long {
    return try {
        val criacao = OffsetDateTime.parse(dataCriacao).toInstant().toEpochMilli()
        val agora = System.currentTimeMillis()
        val diff = 72 * 60 * 60 * 1000L - (agora - criacao)
        diff.coerceAtLeast(0) / (1000 * 60 * 60)
    } catch (e: Exception) {
        0
    }
}