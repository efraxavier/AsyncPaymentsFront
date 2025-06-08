package com.example.asyncpayments.utils

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.asyncpayments.databinding.DialogResponseBinding

object ShowNotification {
    enum class Type {
        TRANSACTION_SENT,
        TRANSACTION_RECEIVED,
        LOGIN_SUCCESS,
        LOGIN_ERROR,
        REGISTER_SUCCESS,
        REGISTER_ERROR,
        SYNC_SUCCESS,
        SYNC_ERROR,
        GENERIC
    }

    fun show(
        context: Context,
        type: Type,
        valor: Double = 0.0,
        extra: String = ""
    ) {
        // Verifica se o contexto é uma atividade e se ela está ativa
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
            return
        }

        val (title, message) = when (type) {
            Type.TRANSACTION_RECEIVED -> "Transação recebida" to extra
            else -> "Aviso" to extra
        }

        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message

        // Cria e exibe o diálogo
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        // Adiciona uma verificação para garantir que o diálogo seja descartado corretamente
        if (context is android.app.Activity && !context.isFinishing && !context.isDestroyed) {
            dialog.show()
        }
    }
}