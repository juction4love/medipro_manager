package com.medipro.manager.data.invoice

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Documents/MediPro/Invoices/ with app-documents fallback when public storage is unavailable. */
    fun invoiceDirectory(): File {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "MediPro/Invoices",
        )
        if (publicDir.exists() || publicDir.mkdirs()) {
            if (publicDir.canWrite()) return publicDir
        }
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
            "MediPro/Invoices",
        ).apply { mkdirs() }
    }

    fun invoiceFile(invoiceNumber: String): File =
        File(invoiceDirectory(), "$invoiceNumber.pdf")

    /** Cache copy for FileProvider fallback on devices with scoped storage quirks. */
    fun cacheInvoiceFile(invoiceNumber: String): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        return File(dir, "$invoiceNumber.pdf")
    }
}
