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
        
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
            return
        }

        val (title, message) = when (type) {
            Type.TRANSACTION_RECEIVED -> "Transação recebida" to extra
            Type.TRANSACTION_SENT -> "Transação enviada" to "Valor: R$ %.2f".format(valor)
            Type.LOGIN_SUCCESS -> "Login realizado" to extra
            Type.LOGIN_ERROR -> "Erro de login" to extra
            Type.REGISTER_SUCCESS -> "Cadastro realizado" to extra
            Type.REGISTER_ERROR -> "Erro de cadastro" to extra
            Type.SYNC_SUCCESS -> "Sincronização concluída" to extra
            Type.SYNC_ERROR -> "Erro de sincronização" to extra
            Type.GENERIC -> "Aviso" to extra
        }

        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message

        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialog.show()
    }
}