package com.example.asyncpayments.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.asyncpayments.R
import com.example.asyncpayments.databinding.ItemTransactionBinding
import com.example.asyncpayments.model.TransactionResponse

class TransactionAdapter(
    private val transactions: List<TransactionResponse>,
    private val tipoConta: String?,
    private val userId: Long? = null 
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: TransactionResponse, isExpanded: Boolean) {
            // Resumo
            binding.tvTransactionId.text = "ID: ${transaction.id}"
            binding.tvTransactionStatus.text = transaction.status
            binding.tvTransactionStatus.setTextColor(getStatusColor(transaction.status, binding.root.context))
            binding.tvTransactionSenderName.text = transaction.nomeUsuarioOrigem
            binding.tvTransactionDate.text = transaction.dataCriacao?.substring(0, 10) ?: "--"
            binding.tvTransactionValor.text = "R$ %.2f".format(transaction.valor)

            // Seta de entrada/saída
            val isSaida = userId != null && transaction.idUsuarioOrigem == userId
            val isEntrada = userId != null && transaction.idUsuarioDestino == userId

            binding.ivArrow.visibility = View.VISIBLE
            if (isSaida) {
                binding.ivArrow.setImageResource(R.drawable.ic_arrow_upward)
                binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.red_accent))
            } else if (isEntrada) {
                binding.ivArrow.setImageResource(R.drawable.ic_arrow_downward)
                binding.ivArrow.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.green_active))
            } else {
                binding.ivArrow.visibility = View.INVISIBLE
            }

            // Expansão
            binding.transactionDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.tvTransactionGateway.text = "Gateway: ${transaction.gatewayPagamento}"
            binding.tvTransactionTipo.text = "Tipo: ${transaction.tipoOperacao}"
            binding.tvTransactionDescricao.text = "Descrição: ${transaction.descricao ?: "--"}"

            binding.root.setOnClickListener {
                if (expandedPositions.contains(adapterPosition)) {
                    expandedPositions.remove(adapterPosition)
                } else {
                    expandedPositions.add(adapterPosition)
                }
                notifyItemChanged(adapterPosition)
            }
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
            // Exiba mensagem de vazio
            (holder as EmptyViewHolder).bind(tipoConta)
        } else {
            // Exiba transação normalmente
            (holder as TransactionViewHolder).bind(transactions[position], expandedPositions.contains(position))
        }
    }

    override fun getItemCount(): Int = if (transactions.isEmpty()) 1 else transactions.size

    override fun getItemViewType(position: Int): Int {
        return if (transactions.isEmpty()) 0 else 1
    }

    // Adicione um ViewHolder para o item vazio
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

// Função utilitária para cor de status
fun getStatusColor(status: String?, context: Context): Int {
    return when (status) {
        "PENDENTE" -> ContextCompat.getColor(context, R.color.orange_primary)
        "SINCRONIZADA" -> ContextCompat.getColor(context, R.color.green_active) 
        "ROLLBACK", "ERRO" -> ContextCompat.getColor(context, R.color.red_accent) 
        else -> ContextCompat.getColor(context, R.color.light_gray) 
    }
}