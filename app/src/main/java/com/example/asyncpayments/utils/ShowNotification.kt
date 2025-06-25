package com.example.asyncpayments.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.asyncpayments.utils.AppLogger
import android.view.LayoutInflater
import android.widget.Toast
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
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            AppLogger.log("ShowNotification", "Usando Toast pois contexto não é Activity ativa: $extra")
            Toast.makeText(context.applicationContext, extra, Toast.LENGTH_LONG).show()
            return
        }

        if (type == Type.LOGIN_ERROR || type == Type.LOGIN_SUCCESS ||
            type == Type.REGISTER_ERROR || type == Type.REGISTER_SUCCESS ||
            type == Type.GENERIC) {
            Toast.makeText(context.applicationContext, extra, Toast.LENGTH_LONG).show()
            return
        }

        try {
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
        } catch (e: Exception) {
            AppLogger.log("ShowNotification", "Erro ao exibir diálogo: ${e.message}", e)
        }

        if (type == Type.GENERIC && extra.contains("rastreabilidade", ignoreCase = true)) {
            AppLogger.log("ShowNotification", "Confirmação de rastreabilidade exibida: $extra")
        }
    }
}