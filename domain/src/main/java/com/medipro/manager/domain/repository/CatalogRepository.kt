package com.medipro.manager.domain.repository

import com.medipro.manager.domain.model.CatalogMedicine
import com.medipro.manager.domain.model.PosSearchResponse
import com.medipro.manager.domain.model.PosSearchResult

interface CatalogRepository {
    suspend fun search(query: String, limit: Int = 40): List<CatalogMedicine>
    suspend fun searchByComposition(composition: String, excludeBrand: String, limit: Int = 12): List<CatalogMedicine>
    suspend fun findByBarcode(barcode: String): CatalogMedicine?
    suspend fun getById(id: Long): CatalogMedicine?
}

interface PosSearchRepository {
    suspend fun search(query: String): PosSearchResponse
    suspend fun findAlternatives(medicineId: Long): List<PosSearchResult>
    suspend fun resolveCatalogItem(catalogId: Long): PosSearchResult?
    suspend fun lookupBarcode(barcode: String): PosSearchResult?
}
