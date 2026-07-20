'use strict';

const { norm, firstToken } = require('./utils');

/**
 * Smart match: Exact Brand → Brand+Strength → Barcode → Generic → Token.
 */
class MedicineMatcher {
  match(excelRow, catalog) {
    // 1. Exact brand / product name
    const exact = catalog.getByExactName(excelRow.productName);
    if (exact.length) {
      const refined = this._refineByStrengthForm(exact, excelRow);
      return { catalog: refined, matchType: 'exact-brand', confidence: 1.0 };
    }

    // 2. Brand + strength (+ form)
    const tokenHits = catalog.getByBrandToken(excelRow.parsedBrand || excelRow.productName);
    if (tokenHits.length) {
      const strengthForm = tokenHits.filter(
        (c) =>
          (!excelRow.parsedStrength || norm(c.strength) === norm(excelRow.parsedStrength)) &&
          (!excelRow.parsedForm || norm(c.dosageForm) === norm(excelRow.parsedForm))
      );
      if (strengthForm.length) {
        return { catalog: this._bestCatalogEntry(strengthForm), matchType: 'brand+strength', confidence: 0.9 };
      }

      const normBrandHits = tokenHits.filter((c) => norm(c.brandName) === excelRow.normBrand);
      if (normBrandHits.length) {
        return { catalog: this._bestCatalogEntry(normBrandHits), matchType: 'brand-label', confidence: 0.75 };
      }
    }

    // 3. Barcode — Excel has none; skip unless future column added

    // 4. Generic — Excel has none; skip

    // 5. Token / fuzzy first-word match
    if (tokenHits.length) {
      return { catalog: this._bestCatalogEntry(tokenHits), matchType: 'token-match', confidence: 0.5 };
    }

    return { catalog: null, matchType: 'missing', confidence: 0 };
  }

  _refineByStrengthForm(candidates, excelRow) {
    if (!excelRow.parsedStrength && !excelRow.parsedForm) {
      return this._bestCatalogEntry(candidates);
    }
    const refined = candidates.filter(
      (c) =>
        (!excelRow.parsedStrength || norm(c.strength) === norm(excelRow.parsedStrength)) &&
        (!excelRow.parsedForm || norm(c.dosageForm) === norm(excelRow.parsedForm))
    );
    return this._bestCatalogEntry(refined.length ? refined : candidates);
  }

  _bestCatalogEntry(candidates) {
    const sorted = [...candidates].sort((a, b) => {
      const score = (c) =>
        (c.hasKnownGeneric ? 4 : 0) +
        (c.hasKnownManufacturer ? 2 : 0) +
        (c.barcode ? 1 : 0);
      return score(b) - score(a);
    });
    return sorted[0];
  }
}

module.exports = { MedicineMatcher };
