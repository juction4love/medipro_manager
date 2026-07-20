'use strict';

const { sqlRows } = require('./sqlite-cli');
const { norm, firstToken, isUnknown } = require('./utils');

class LegacyCatalogReader {
  constructor(dbPath) {
    this.dbPath = dbPath;
    this.records = [];
    this.byNormBrand = new Map();
    this.byNormName = new Map();
    this.byBarcode = new Map();
    this.byGeneric = new Map();
    this.byManufacturer = new Map();
    this.byBrandToken = new Map();
  }

  read() {
    const batchSize = 50000;
    const total = parseInt(sqlRows(this.dbPath, 'SELECT COUNT(*) c FROM medicines')[0].c, 10);

    for (let offset = 0; offset < total; offset += batchSize) {
      const rows = sqlRows(
        this.dbPath,
        `SELECT id, brand_name, generic_name, category, strength, dosage_form, manufacturer, barcode, purchase_price, mrp FROM medicines LIMIT ${batchSize} OFFSET ${offset}`
      );
      for (const row of rows) {
        const rec = this._toCatalogMedicine(row);
        this.records.push(rec);
        this._index(rec);
      }
      process.stderr.write(`  Indexed ${Math.min(offset + batchSize, total)} / ${total}\r`);
    }
    process.stderr.write('\n');

    return {
      total: this.records.length,
      indexes: {
        byNormBrand: this.byNormBrand.size,
        byNormName: this.byNormName.size,
        byBarcode: this.byBarcode.size,
        byGeneric: this.byGeneric.size,
        byManufacturer: this.byManufacturer.size,
        byBrandToken: this.byBrandToken.size,
      },
    };
  }

  _toCatalogMedicine(row) {
    const brandName = String(row.brand_name ?? '').trim();
    const genericName = String(row.generic_name ?? '').trim() || 'Unknown';
    const manufacturer = String(row.manufacturer ?? '').trim() || 'Unknown';
    const strength = String(row.strength ?? '').trim();
    const dosageForm = String(row.dosage_form ?? '').trim() || 'Tablet';
    const barcode = String(row.barcode ?? '').trim() || null;

    return {
      legacyId: row.id,
      brandName,
      genericName,
      composition: isUnknown(genericName) ? '' : genericName,
      strength,
      dosageForm,
      manufacturer,
      category: String(row.category ?? 'General').trim() || 'General',
      barcode,
      purchasePrice: row.purchase_price ?? 0,
      mrp: row.mrp ?? 0,
      normName: norm(brandName),
      normBrand: norm(brandName),
      normGeneric: norm(genericName),
      normManufacturer: norm(manufacturer),
      normBarcode: barcode ? norm(barcode) : '',
      normStrength: norm(strength),
      normForm: norm(dosageForm),
      brandToken: firstToken(brandName),
      hasKnownGeneric: !isUnknown(genericName),
      hasKnownManufacturer: !isUnknown(manufacturer),
    };
  }

  _index(rec) {
    this._push(this.byNormName, rec.normName, rec);
    this._push(this.byNormBrand, rec.normBrand, rec);
    if (rec.normBarcode) this._push(this.byBarcode, rec.normBarcode, rec);
    if (rec.hasKnownGeneric) this._push(this.byGeneric, rec.normGeneric, rec);
    if (rec.hasKnownManufacturer) this._push(this.byManufacturer, rec.normManufacturer, rec);
    this._push(this.byBrandToken, rec.brandToken, rec);
  }

  _push(map, key, rec) {
    if (!key) return;
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(rec);
  }

  getByExactName(productName) {
    return this.byNormName.get(norm(productName)) ?? [];
  }

  getByBrandToken(token) {
    return this.byBrandToken.get(firstToken(token)) ?? [];
  }

  getByBarcode(barcode) {
    if (!barcode) return [];
    return this.byBarcode.get(norm(barcode)) ?? [];
  }
}

module.exports = { LegacyCatalogReader };
