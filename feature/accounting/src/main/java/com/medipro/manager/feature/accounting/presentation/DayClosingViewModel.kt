package com.medipro.manager.feature.accounting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.DayClosingPreview
import com.medipro.manager.domain.model.DayClosingReason
import com.medipro.manager.domain.model.DayClosingRecord
import com.medipro.manager.domain.usecase.accounting.CloseDayUseCase
import com.medipro.manager.domain.usecase.accounting.GenerateDayClosingPdfUseCase
import com.medipro.manager.domain.usecase.accounting.GetDayClosingPreviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class DayClosingState(
    val preview: DayClosingPreview? = null,
    val isLoading: Boolean = true,
    val actualCashInput: String = "",
    val remarks: String = "",
    val differenceReason: String? = null,
    val isClosing: Boolean = false,
    val closedRecord: DayClosingRecord? = null,
    val pdfPath: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val actualCash: Double?
        get() = actualCashInput.toDoubleOrNull()

    val difference: Double?
        get() {
            val preview = preview ?: return null
            val actual = actualCash ?: return null
            return actual - preview.expectedCash
        }

    val needsDifferenceReason: Boolean
        get() = difference?.let { abs(it) > 0.01 } == true

    val canClose: Boolean
        get() = preview != null &&
            !preview.isAlreadyClosed &&
            actualCash != null &&
            !isClosing &&
            (!needsDifferenceReason || differenceReason != null)
}

@HiltViewModel
class DayClosingViewModel @Inject constructor(
    private val getPreview: GetDayClosingPreviewUseCase,
    private val closeDay: CloseDayUseCase,
    private val generatePdf: GenerateDayClosingPdfUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DayClosingState())
    val state: StateFlow<DayClosingState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { getPreview() }
                .onSuccess { preview ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            preview = preview,
                            closedRecord = preview.closedRecord,
                            actualCashInput = if (preview.isAlreadyClosed) {
                                preview.closedRecord?.actualCash?.toString().orEmpty()
                            } else {
                                preview.expectedCash.toString()
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load preview")
                    }
                }
        }
    }

    fun onActualCashChange(value: String) = _state.update {
        it.copy(actualCashInput = value, errorMessage = null)
    }

    fun onRemarksChange(value: String) = _state.update { it.copy(remarks = value) }

    fun onDifferenceReasonChange(reason: String) = _state.update {
        it.copy(differenceReason = reason, errorMessage = null)
    }

    fun closeDay() {
        val current = _state.value
        val actual = current.actualCash ?: return
        if (current.preview?.isAlreadyClosed == true) return

        viewModelScope.launch {
            _state.update { it.copy(isClosing = true, errorMessage = null) }
            runCatching {
                closeDay(
                    actualCash = actual,
                    remarks = current.remarks.takeIf { it.isNotBlank() },
                    differenceReason = current.differenceReason?.takeIf { current.needsDifferenceReason },
                )
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isClosing = false,
                        closedRecord = result.record,
                        pdfPath = result.pdfPath,
                        successMessage = "Day closed: ${result.record.reference}",
                    )
                }
                refresh()
            }.onFailure { e ->
                _state.update {
                    it.copy(isClosing = false, errorMessage = e.message ?: "Failed to close day")
                }
            }
        }
    }

    fun regeneratePdf() {
        val closingId = _state.value.closedRecord?.id ?: return
        viewModelScope.launch {
            runCatching { generatePdf(closingId) }
                .onSuccess { path ->
                    _state.update { it.copy(pdfPath = path, successMessage = "Day report PDF saved") }
                }
                .onFailure { e ->
                    _state.update { it.copy(errorMessage = e.message ?: "PDF generation failed") }
                }
        }
    }

    fun clearMessages() = _state.update { it.copy(errorMessage = null, successMessage = null) }
}
