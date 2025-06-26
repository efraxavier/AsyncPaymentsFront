package com.example.asyncpayments.ui.transaction

object TransactionStatusMapper {
    fun map(status: String): String {
        return when (status) {
            "PENDENTE" -> "Pendente"
            "SINCRONIZADA" -> "Sincronizada"
            "ROLLBACK" -> "Revertida"
            "ERRO" -> "Erro"
            else -> "Desconhecido"
        }
    }
}