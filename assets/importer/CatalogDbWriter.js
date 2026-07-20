'use strict';

const fs = require('fs');
const { createDb, insertBatch, sqlFile } = require('./sqlite-cli');
const { CATALOG_DDL } = require('./schema');
const { isUnknown } = require('./utils');
const { encodePhonetic } = require('./phonetic');

/** Static synonym seeds — expanded at runtime by Kotlin SynonymDictionary too. */
const SYNONYM_SEEDS = [
  ['paracetamol', 'pcm'], ['paracetamol', 'acetaminophen'], ['paracetamol', 'parasetamol'],
  ['ibuprofen', 'ibu'], ['amoxicillin', 'amox'], ['cetirizine', 'citrizine'],
  ['omeprazole', 'ome'], ['pantoprazole', 'pantop'], ['metformin', 'met'],
  ['azithromycin', 'azithro'], ['diclofenac', 'diclo'], ['salbutamol', 'salb'],
  ['tablet', 'tab'], ['capsule', 'cap'], ['syrup', 'syp'], ['injection', 'inj'],
];

function buildCompositionTokens(composition, genericName) {
  const source = `${composition || ''} ${genericName || ''}`.toLowerCase();
  const tokens = source
    .replace(/[^a-z0-9+\\s]/g, ' ')
    .split(/[+\\s]+/)
    .map((t) => t.trim())
    .filter((t) => t.length >= 3);
  return [...new Set(tokens)].join(' ');
}

class CatalogDbWriter {
  constructor(outputPath) {
    this.outputPath = outputPath;
  }

  write(catalogRecords) {
    createDb(this.outputPath, CATALOG_DDL);

    const columns = [
      'id', 'medicineUuid', 'brandName', 'genericName', 'composition', 'compositionTokens',
      'strength', 'dosageForm', 'manufacturer', 'category', 'barcode', 'phoneticBrand', 'phoneticGeneric',
    ];
    const rows = catalogRecords.map((r) => {
      const brandName = r.brandName;
      const genericName = isUnknown(r.genericName) ? 'Unknown' : r.genericName;
      const composition = r.composition || (isUnknown(r.genericName) ? '' : r.genericName);
      return {
        id: r.legacyId,
        medicineUuid: r.uuid || String(r.legacyId),
        brandName,
        genericName,
        composition,
        compositionTokens: buildCompositionTokens(composition, genericName),
        strength: r.strength || '',
        dosageForm: r.dosageForm || 'Tablet',
        manufacturer: isUnknown(r.manufacturer) ? 'Unknown' : r.manufacturer,
        category: r.category || 'General',
        barcode: r.barcode,
        phoneticBrand: encodePhonetic(brandName),
        phoneticGeneric: encodePhonetic(genericName),
      };
    });

    insertBatch(this.outputPath, 'catalog_medicines', columns, rows, 1000);

    const synonymRows = [];
    const seen = new Set();
    SYNONYM_SEEDS.forEach(([canonical, term]) => {
      const key = `${term}|${canonical}`;
      if (!seen.has(key)) {
        seen.add(key);
        synonymRows.push({ term, canonical });
      }
      const reverse = `${canonical}|${term}`;
      if (!seen.has(reverse)) {
        seen.add(reverse);
        synonymRows.push({ term: canonical, canonical: term });
      }
    });
    insertBatch(this.outputPath, 'catalog_synonyms', ['term', 'canonical'], synonymRows, 500);

    const count = parseInt(sqlFile(this.outputPath, 'SELECT COUNT(*) FROM catalog_medicines;'), 10);
    return {
      path: this.outputPath,
      count,
      sizeMb: +(fs.statSync(this.outputPath).size / 1024 / 1024).toFixed(2),
    };
  }
}

module.exports = { CatalogDbWriter };
