'use strict';

const { norm, firstToken, isUnknown } = require('./utils');

class MedicineEnricher {
  enrich(excelRow, matchResult, catalog) {
    let source = matchResult.catalog;
    let enrichedFrom = null;

    if (source && (!source.hasKnownGeneric || !source.hasKnownManufacturer)) {
      const better = this._findBetterCatalogEntry(excelRow, source, catalog);
      if (better) {
        enrichedFrom = {
          legacyId: better.legacyId,
          brandName: better.brandName,
          genericName: better.genericName,
          manufacturer: better.manufacturer,
        };
        source = { ...source, ...this._mergeKnownFields(source, better) };
      }
    }

    const brandName = excelRow.productName;
    const genericName = source?.genericName && !isUnknown(source.genericName)
      ? source.genericName
      : 'Unknown';
    const manufacturer = source?.manufacturer && !isUnknown(source.manufacturer)
      ? source.manufacturer
      : 'Unknown';
    const composition = source?.composition && !isUnknown(source.composition)
      ? source.composition
      : genericName !== 'Unknown' ? genericName : '';
    const strength = source?.strength || excelRow.parsedStrength || '';
    const dosageForm = source?.dosageForm || excelRow.parsedForm || 'Tablet';
    const category = source?.category || 'General';
    const barcode = source?.barcode || null;

    return {
      brandName,
      genericName,
      composition,
      strength,
      dosageForm,
      manufacturer,
      category,
      barcode,
      enrichedFrom,
      needsReview: genericName === 'Unknown' || manufacturer === 'Unknown',
      matchType: matchResult.matchType,
      confidence: matchResult.confidence,
      legacyCatalogId: source?.legacyId ?? null,
    };
  }

  _findBetterCatalogEntry(excelRow, current, catalog) {
    const token = firstToken(excelRow.parsedBrand || excelRow.productName);
    const candidates = catalog.getByBrandToken(token).filter(
      (c) => c.legacyId !== current.legacyId && (c.hasKnownGeneric || c.hasKnownManufacturer)
    );
    if (!candidates.length) return null;

    const scored = candidates
      .map((c) => {
        let score = 0;
        if (c.hasKnownGeneric) score += 3;
        if (c.hasKnownManufacturer) score += 2;
        if (excelRow.parsedStrength && norm(c.strength) === norm(excelRow.parsedStrength)) score += 2;
        if (excelRow.parsedForm && norm(c.dosageForm) === norm(excelRow.parsedForm)) score += 1;
        if (norm(c.brandName) === excelRow.normBrand) score += 1;
        return { c, score };
      })
      .sort((a, b) => b.score - a.score);

    return scored[0]?.score > 0 ? scored[0].c : null;
  }

  _mergeKnownFields(current, better) {
    return {
      genericName: !isUnknown(better.genericName) ? better.genericName : current.genericName,
      composition: !isUnknown(better.composition) ? better.composition : current.composition,
      manufacturer: !isUnknown(better.manufacturer) ? better.manufacturer : current.manufacturer,
      strength: current.strength || better.strength,
      dosageForm: current.dosageForm || better.dosageForm,
      category: current.category || better.category,
      barcode: current.barcode || better.barcode,
      hasKnownGeneric: current.hasKnownGeneric || better.hasKnownGeneric,
      hasKnownManufacturer: current.hasKnownManufacturer || better.hasKnownManufacturer,
    };
  }
}

module.exports = { MedicineEnricher };
