'use strict';

const { isUnknown, uuid } = require('./utils');
const { PriceConverter } = require('./PriceConverter');

/**
 * Builds MedicineEntity rows. Duplicate catalog stubs (Unknown generic) are resolved
 * during enrichment — output keeps one row per Excel product.
 */
class DuplicateResolver {
  static toMedicineEntity(enriched, salesPriceNpr, now = Date.now()) {
    const paisa = PriceConverter.toPaisa(salesPriceNpr);
    return {
      uuid: uuid(),
      brandName: enriched.brandName,
      genericName: enriched.genericName,
      composition: enriched.composition || enriched.genericName,
      strength: enriched.strength || '',
      dosageForm: enriched.dosageForm || 'Tablet',
      manufacturer: enriched.manufacturer,
      category: enriched.category || 'General',
      barcode: enriched.barcode,
      unit: 'pcs',
      purchasePricePaisa: 0,
      sellingPricePaisa: paisa,
      mrpPaisa: paisa,
      vatPercent: 13.0,
      reorderLevel: 10,
      description: null,
      requiresPrescription: 0,
      controlledSubstance: 0,
      scheduleCategory: 'OTC',
      isActive: 1,
      createdAt: now,
      updatedAt: now,
      deletedAt: null,
      syncStatus: 'PENDING',
      syncVersion: 0,
      deviceId: null,
      _meta: {
        matchType: enriched.matchType,
        needsReview: enriched.needsReview,
        legacyCatalogId: enriched.legacyCatalogId,
        enrichedFrom: enriched.enrichedFrom,
        excelRowNum: enriched.excelRowNum,
        replacedUnknownStub: !!enriched.enrichedFrom,
      },
    };
  }

  /** Stats only — catalog Unknown stubs replaced during enrichment, not dropped from inventory. */
  static countEnrichmentReplacements(entities) {
    return entities.filter((e) => e._meta?.replacedUnknownStub).length;
  }
}

module.exports = { DuplicateResolver };
