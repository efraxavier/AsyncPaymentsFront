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
        val (title, message) = when (type) {
            Type.TRANSACTION_SENT ->
                "Transação enviada" to extra
            Type.TRANSACTION_RECEIVED ->
                "Transação recebida" to extra
            Type.LOGIN_SUCCESS ->
                "Login realizado" to "Bem-vindo!"
            Type.LOGIN_ERROR ->
                "Erro no login" to extra
            Type.REGISTER_SUCCESS ->
                "Cadastro realizado" to "Cadastro realizado com sucesso! Você será redirecionado."
            Type.REGISTER_ERROR ->
                "Erro no cadastro" to extra
            Type.SYNC_SUCCESS ->
                "Sincronização" to extra
            Type.SYNC_ERROR ->
                "Erro de sincronização" to extra
            Type.GENERIC ->
                "Aviso" to extra
        }
        val dialogBinding = DialogResponseBinding.inflate(LayoutInflater.from(context))
        dialogBinding.tvDialogTitle.text = title
        dialogBinding.tvDialogMessage.text = message
        AlertDialog.Builder(context)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
            .show()
    }
}