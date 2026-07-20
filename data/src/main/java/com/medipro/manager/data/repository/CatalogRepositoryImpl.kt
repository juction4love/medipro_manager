package com.medipro.manager.data.repository

import com.medipro.manager.data.catalog.CatalogDatabaseHelper
import com.medipro.manager.domain.model.CatalogMedicine
import com.medipro.manager.domain.repository.CatalogRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepositoryImpl @Inject constructor(
    private val catalogDb: CatalogDatabaseHelper,
) : CatalogRepository {
    override suspend fun search(query: String, limit: Int): List<CatalogMedicine> =
        catalogDb.search(query, limit)

    override suspend fun searchByComposition(
        composition: String,
        excludeBrand: String,
        limit: Int,
    ): List<CatalogMedicine> = catalogDb.searchByComposition(composition, excludeBrand, limit)

    override suspend fun findByBarcode(barcode: String): CatalogMedicine? =
        catalogDb.findByBarcode(barcode)

    override suspend fun getById(id: Long): CatalogMedicine? = catalogDb.getById(id)
}
