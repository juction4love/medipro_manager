'use strict';

const fs = require('fs');
const path = require('path');
const { isUnknown } = require('./utils');

class ImportReport {
  constructor() {
    this.stats = {
      catalogTotal: 0,
      excelTotal: 0,
      imported: 0,
      matched: 0,
      missing: 0,
      duplicatesRemoved: 0,
      priceUpdated: 0,
      unknownGeneric: 0,
      unknownManufacturer: 0,
      needsReview: 0,
      enrichedFromCatalog: 0,
    };
    this.matchTypes = {};
    this.missingSamples = [];
    this.enrichedSamples = [];
    this.duplicateRemovedSamples = [];
  }

  recordMatch(matchType) {
    this.matchTypes[matchType] = (this.matchTypes[matchType] || 0) + 1;
    if (matchType !== 'missing') this.stats.matched++;
    else this.stats.missing++;
  }

  recordEnrichmentReplacement(entity) {
    if (entity._meta?.replacedUnknownStub) {
      this.stats.enrichedFromCatalog++;
      if (this.enrichedSamples.length < 20) {
        this.enrichedSamples.push({
          brandName: entity.brandName,
          genericName: entity.genericName,
          manufacturer: entity.manufacturer,
          enrichedFrom: entity._meta.enrichedFrom,
        });
      }
    }
  }

  recordEntity(entity) {
    if (isUnknown(entity.genericName)) this.stats.unknownGeneric++;
    if (isUnknown(entity.manufacturer)) this.stats.unknownManufacturer++;
    if (entity._meta?.needsReview) this.stats.needsReview++;
    this.recordEnrichmentReplacement(entity);
    if (entity.sellingPricePaisa > 0) this.stats.priceUpdated++;
  }

  recordMissing(excelRow) {
    if (this.missingSamples.length < 50) {
      this.missingSamples.push({
        rowNum: excelRow.rowNum,
        productName: excelRow.productName,
        salesPrice: excelRow.salesPrice,
      });
    }
  }

  recordDuplicates(removed) {
    this.stats.duplicatesRemoved = removed.length;
    this.duplicateRemovedSamples = removed.slice(0, 20).map((r) => ({
      reason: r.reason,
      brandName: r.item.brandName,
      genericName: r.item.genericName,
    }));
  }

  finalize(seedResult, catalogResult) {
    this.stats.imported = seedResult.count;
    this.generatedAt = new Date().toISOString();
    this.outputs = { seedDb: seedResult, catalogDb: catalogResult };
    return this.toJSON();
  }

  toJSON() {
    return {
      generatedAt: this.generatedAt,
      summary: this.stats,
      matchTypes: this.matchTypes,
      missingSamples: this.missingSamples,
      enrichedSamples: this.enrichedSamples,
      duplicateRemovedSamples: this.duplicateRemovedSamples,
      outputs: this.outputs,
    };
  }

  write(outputPath) {
    const json = this.finalize(this.outputs?.seedDb ?? { count: this.stats.imported }, this.outputs?.catalogDb ?? { count: this.stats.catalogTotal });
    const dir = path.dirname(outputPath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(outputPath, JSON.stringify(json, null, 2));
    return outputPath;
  }
}

module.exports = { ImportReport };
