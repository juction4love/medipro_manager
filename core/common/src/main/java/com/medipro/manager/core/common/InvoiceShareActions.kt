package com.medipro.manager.core.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

enum class InvoiceShareTarget {
    SHARE_SHEET,
    WHATSAPP,
}

object InvoiceShareActions {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun sharePdf(context: Context, filePath: String, target: InvoiceShareTarget) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Invoice PDF not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        if (target == InvoiceShareTarget.WHATSAPP) {
            for (pkg in WHATSAPP_PACKAGES) {
                val intent = baseShareIntent(uri, file).apply { setPackage(pkg) }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            }
            sharePdf(context, filePath, InvoiceShareTarget.SHARE_SHEET)
            return
        }
        context.startActivity(Intent.createChooser(baseShareIntent(uri, file), "Share Invoice"))
    }

    private fun baseShareIntent(uri: android.net.Uri, file: File) = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Invoice ${file.nameWithoutExtension}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun notifySaved(context: Context, filePath: String) {
        Toast.makeText(context, "Saved: ${File(filePath).name}", Toast.LENGTH_SHORT).show()
    }

    fun notifyThermalResult(context: Context, result: kotlin.Result<Unit>) {
        result.fold(
            onSuccess = {
                Toast.makeText(context, "Sent to thermal printer", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(
                    context,
                    error.message ?: "Bluetooth thermal printing — connect printer in Settings",
                    Toast.LENGTH_LONG,
                ).show()
            },
        )
    }
}
