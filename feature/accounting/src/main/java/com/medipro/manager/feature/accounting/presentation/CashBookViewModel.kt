package com.medipro.manager.feature.accounting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.ExportFormat
import com.medipro.manager.domain.usecase.accounting.ExportCashBookUseCase
import com.medipro.manager.domain.usecase.accounting.ObserveTodayCashBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CashBookState(
    val entries: List<com.medipro.manager.domain.model.CashBookEntry> = emptyList(),
    val openingBalance: Double = 0.0,
    val closingBalance: Double = 0.0,
    val isExporting: Boolean = false,
) {
    val totalIn: Double
        get() = entries.sumOf { it.cashIn }

    val totalOut: Double
        get() = entries.sumOf { it.cashOut }

    val dateLabel: String
        get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
}

sealed class CashBookEvent {
    data class ShareExport(val path: String, val mimeType: String) : CashBookEvent()
}

@HiltViewModel
class CashBookViewModel @Inject constructor(
    observeTodayCashBook: ObserveTodayCashBookUseCase,
    private val exportCashBook: ExportCashBookUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CashBookState())
    val state: StateFlow<CashBookState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CashBookEvent>()
    val events: SharedFlow<CashBookEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeTodayCashBook().collect { entries ->
                val totalIn = entries.sumOf { it.cashIn }
                val totalOut = entries.sumOf { it.cashOut }
                var running = 0.0
                entries.forEach { entry ->
                    running += entry.cashIn - entry.cashOut
                }
                _state.update {
                    it.copy(
                        entries = entries,
                        openingBalance = running - totalIn + totalOut,
                        closingBalance = running,
                    )
                }
            }
        }
    }

    fun exportPdf() = export(ExportFormat.PDF)

    fun exportCsv() = export(ExportFormat.CSV)

    private fun export(format: ExportFormat) {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            runCatching {
                exportCashBook(
                    entries = current.entries,
                    openingBalance = current.openingBalance,
                    closingBalance = current.closingBalance,
                    dateLabel = current.dateLabel,
                    format = format,
                )
            }.onSuccess { file ->
                val mime = when (format) {
                    ExportFormat.PDF -> "application/pdf"
                    ExportFormat.CSV -> "text/csv"
                }
                _events.emit(CashBookEvent.ShareExport(file.absolutePath, mime))
            }
            _state.update { it.copy(isExporting = false) }
        }
    }
}
