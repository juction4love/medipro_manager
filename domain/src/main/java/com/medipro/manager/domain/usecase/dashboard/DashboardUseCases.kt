package com.medipro.manager.domain.usecase.dashboard

import com.medipro.manager.domain.model.DashboardSnapshot
import com.medipro.manager.domain.model.DashboardStats
import com.medipro.manager.domain.model.Sale
import com.medipro.manager.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDashboardUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<DashboardSnapshot> = repository.observeDashboard()
}

class ObserveDashboardStatsUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<DashboardStats> = repository.observeStats()
}

class ObserveRecentSalesUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<List<Sale>> = repository.observeRecentSales()
}
