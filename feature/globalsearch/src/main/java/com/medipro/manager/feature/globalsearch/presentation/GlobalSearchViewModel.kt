package com.medipro.manager.feature.globalsearch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medipro.manager.domain.model.GlobalSearchResponse
import com.medipro.manager.domain.repository.GlobalSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class GlobalSearchUiState(
    val query: String = "",
    val results: GlobalSearchResponse = GlobalSearchResponse(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val repository: GlobalSearchRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val state = queryFlow
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            flow {
                if (query.trim().length < 2) {
                    emit(
                        GlobalSearchUiState(
                            query = query,
                            results = GlobalSearchResponse(),
                            isLoading = false,
                            hasSearched = false,
                        ),
                    )
                    return@flow
                }
                emit(GlobalSearchUiState(query = query, isLoading = true, hasSearched = true))
                val results = repository.search(query)
                emit(
                    GlobalSearchUiState(
                        query = query,
                        results = results,
                        isLoading = false,
                        hasSearched = true,
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GlobalSearchUiState(),
        )

    fun onQueryChange(query: String) {
        queryFlow.update { query }
    }
}
