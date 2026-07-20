package com.medipro.manager.data.export

import android.content.Context
import com.medipro.manager.data.document.report.CashBookPdfGenerator
import com.medipro.manager.data.document.report.CashBookRenderInput
import com.medipro.manager.domain.model.CashBookEntry
import com.medipro.manager.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashBookExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cashBookPdfGenerator: CashBookPdfGenerator,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun exportPdf(
        entries: List<CashBookEntry>,
        openingBalance: Double,
        closingBalance: Double,
        dateLabel: String,
    ): File {
        val settings = settingsRepository.getSettings()
        return cashBookPdfGenerator.generate(
            CashBookRenderInput(
                entries = entries,
                openingBalance = openingBalance,
                closingBalance = closingBalance,
                dateLabel = dateLabel,
                settings = settings,
            ),
        )
    }

    fun exportCsv(
        entries: List<CashBookEntry>,
        openingBalance: Double,
        closingBalance: Double,
        dateLabel: String,
    ): File {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val file = File(context.cacheDir, "cashbook_${System.currentTimeMillis()}.csv")
        val lines = buildList {
            add("Cash Book,$dateLabel")
            add("")
            add("Opening Balance,$openingBalance")
            add("Time,Description,Reference,Cash In,Cash Out")
            entries.forEach { entry ->
                add(
                    listOf(
                        timeFmt.format(Date(entry.entryDate)),
                        csvEscape(entry.description),
                        csvEscape(entry.referenceType.orEmpty()),
                        entry.cashIn.toString(),
                        entry.cashOut.toString(),
                    ).joinToString(","),
                )
            }
            add("")
            add("Closing Balance,$closingBalance")
        }
        file.writeText(lines.joinToString("\n"))
        return file
    }

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
}
