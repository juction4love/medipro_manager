/**
 * Full analysis: legacy medipro.db vs product_list.xlsx
 */
const fs = require('fs');
const { execSync } = require('child_process');
const XLSX = require('xlsx');

const SQLITE = 'G:\\Android\\Sdk\\platform-tools\\sqlite3.exe';
const DB_PATH = 'G:\\medipro\\assets\\medipro.db';
const XLSX_PATH = 'G:\\medipro\\assets\\product_list.xlsx';
const OUT_JSON = 'G:\\medipro\\assets\\analysis-report.json';
const OUT_CSV = 'G:\\medipro\\assets\\medicine-import-template.csv';

function sql(query) {
  return execSync(`"${SQLITE}" "${DB_PATH}" "${query.replace(/"/g, '""')}"`, {
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
  }).trim();
}

function sqlRows(query) {
  const out = execSync(`"${SQLITE}" -header -json "${DB_PATH}" "${query.replace(/"/g, '""')}"`, {
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
  });
  if (!out.trim()) return [];
  return JSON.parse(out);
}

function norm(s) {
  return String(s ?? '').trim().toLowerCase().replace(/\s+/g, ' ');
}

function toPaisa(val) {
  const n = parseFloat(String(val ?? '').replace(/,/g, ''));
  return Number.isNaN(n) || n <= 0 ? 0 : Math.round(n * 100);
}

const DOSAGE_FORMS = [
  'TABLET', 'TAB', 'CAPSULE', 'CAP', 'INJECTION', 'INJ', 'SYRUP', 'SYP', 'SUSPENSION', 'SUSP',
  'CREAM', 'GEL', 'OINTMENT', 'DROPS', 'SPRAY', 'POWDER', 'PWD', 'LOTION', 'SOLUTION', 'SOL',
  'INHALER', 'SUPPOSITORY', 'SUP', 'PATCH', 'SACHET', 'VIAL', 'AMP', 'AMPOULE', 'SET',
];

function parseProductName(raw) {
  let name = String(raw ?? '').trim().replace(/\s+/g, ' ');
  const upper = name.toUpperCase();
  let dosageForm = '';
  for (const form of [...DOSAGE_FORMS].sort((a, b) => b.length - a.length)) {
    const re = new RegExp(`\\b${form}\\b`, 'i');
    if (re.test(upper)) {
      dosageForm = { TAB: 'Tablet', CAP: 'Capsule', INJ: 'Injection', SYP: 'Syrup', SUSP: 'Suspension', PWD: 'Powder', SET: 'Set' }[form.toUpperCase()] || form;
      name = name.replace(re, ' ').replace(/\s+/g, ' ').trim();
      break;
    }
  }
  let strength = '';
  const sm = name.match(/\b(\d+(?:\.\d+)?(?:MG|GM|G|ML|MCG|IU|%|W\/W)(?:\/\d+(?:\.\d+)?(?:MG|ML))?)\b/i)
    || name.match(/\b(\d+(?:\.\d+)?)\s*(ML|MG|GM|G)\b/i);
  if (sm) {
    strength = sm[0].replace(/\s+/g, '').toUpperCase();
    name = name.replace(sm[0], ' ').replace(/\s+/g, ' ').trim();
  } else {
    const numOnly = name.match(/\b(\d+(?:\.\d+)?)\s*$/);
    if (numOnly) {
      strength = numOnly[1];
      name = name.replace(numOnly[0], '').trim();
    }
  }
  return { brandName: name.trim(), strength, dosageForm, raw: String(raw ?? '').trim() };
}

// Excel load
const wb = XLSX.readFile(XLSX_PATH);
const sheetName = wb.SheetNames[0];
const rows = XLSX.utils.sheet_to_json(wb.Sheets[sheetName], { defval: '' });
const headers = Object.keys(rows[0] || {});

const excelRecords = rows.map((r, idx) => {
  const productName = String(r.Product_name ?? '').trim();
  const salesPrice = parseFloat(String(r.Sales_price ?? '0').replace(/,/g, '')) || 0;
  const parsed = parseProductName(productName);
  return {
    rowNum: idx + 2,
    productName,
    salesPrice,
    brandName: parsed.brandName,
    strength: parsed.strength,
    dosageForm: parsed.dosageForm,
    normName: norm(productName),
    normBrand: norm(parsed.brandName),
  };
}).filter((r) => r.productName);

// DB index by normalized brand_name (full label)
console.error('Loading DB brand index...');
const dbByNormName = new Map();
const dbByNormBrand = new Map();
const batchSize = 50000;
const dbCount = parseInt(sql('SELECT COUNT(*) FROM medicines;'), 10);

for (let offset = 0; offset < dbCount; offset += batchSize) {
  const batch = sqlRows(`SELECT id, brand_name, generic_name, manufacturer, strength, dosage_form, purchase_price, mrp, barcode, category FROM medicines LIMIT ${batchSize} OFFSET ${offset}`);
  for (const r of batch) {
    const rec = {
      id: r.id,
      brandName: r.brand_name?.trim() ?? '',
      genericName: r.generic_name ?? '',
      manufacturer: r.manufacturer ?? '',
      strength: r.strength ?? '',
      dosageForm: r.dosage_form ?? '',
      purchasePrice: r.purchase_price ?? 0,
      mrp: r.mrp ?? 0,
      barcode: r.barcode ?? '',
      category: r.category ?? 'General',
    };
    const nk = norm(rec.brandName);
    if (!dbByNormName.has(nk)) dbByNormName.set(nk, []);
    dbByNormName.get(nk).push(rec);
    const firstWord = norm(rec.brandName.split(/\s+/)[0]);
    if (!dbByNormBrand.has(firstWord)) dbByNormBrand.set(firstWord, []);
    dbByNormBrand.get(firstWord).push(rec);
  }
}
console.error(`Indexed ${dbByNormName.size} unique brand_name keys from ${dbCount} rows`);

function findDbMatch(ex) {
  const exact = dbByNormName.get(ex.normName);
  if (exact?.length) return { db: exact[0], matchType: 'exact-product-name', alts: exact.length };

  const brandHits = dbByNormName.get(ex.normBrand);
  if (brandHits?.length) {
    const refined = brandHits.find((c) => norm(c.strength) === norm(ex.strength) && norm(c.dosageForm) === norm(ex.dosageForm));
    if (refined) return { db: refined, matchType: 'brand+strength+form', alts: brandHits.length };
    return { db: brandHits[0], matchType: 'brand-label-partial', alts: brandHits.length };
  }

  const fw = ex.normBrand.split(' ')[0];
  const fwHits = dbByNormBrand.get(fw);
  if (fwHits?.length) return { db: fwHits[0], matchType: 'first-token-fuzzy', alts: fwHits.length };

  return null;
}

const matched = [];
const unmatchedExcel = [];
const priceEnrichment = [];

for (const ex of excelRecords) {
  const m = findDbMatch(ex);
  if (m) {
    matched.push({ excel: ex, ...m });
    if (ex.salesPrice > 0 && !(m.db.mrp > 0)) {
      priceEnrichment.push({
        dbId: m.db.id,
        excelProductName: ex.productName,
        dbBrandName: m.db.brandName,
        genericName: m.db.genericName,
        manufacturer: m.db.manufacturer,
        salesPrice: ex.salesPrice,
        matchType: m.matchType,
      });
    }
  } else {
    unmatchedExcel.push(ex);
  }
}

// Excel names set for reverse lookup
const excelNormNames = new Set(excelRecords.map((r) => r.normName));
const excelNormBrands = new Set(excelRecords.map((r) => r.normBrand));

const unmatchedDb = [];
for (const [nk, list] of dbByNormName.entries()) {
  const first = list[0];
  const fw = norm(first.brandName.split(/\s+/)[0]);
  if (!excelNormNames.has(nk) && !excelNormBrands.has(nk) && !excelNormBrands.has(fw)) {
    unmatchedDb.push(first);
    if (unmatchedDb.length >= 200) break;
  }
}

const dbStats = sqlRows(`
  SELECT COUNT(*) total,
    COUNT(DISTINCT brand_name) distinct_brands,
    COUNT(DISTINCT generic_name) distinct_generics,
    COUNT(DISTINCT manufacturer) distinct_manufacturers,
    SUM(CASE WHEN purchase_price > 0 THEN 1 ELSE 0 END) with_purchase,
    SUM(CASE WHEN mrp > 0 THEN 1 ELSE 0 END) with_mrp,
    SUM(CASE WHEN TRIM(barcode) != '' THEN 1 ELSE 0 END) with_barcode
  FROM medicines;
`)[0];

function toImport(ex, db = null) {
  const selling = ex.salesPrice || db?.mrp || 0;
  return {
    brandName: db?.brandName || ex.productName,
    genericName: db?.genericName || 'Unknown',
    composition: db?.genericName || '',
    strength: db?.strength || ex.strength || '',
    dosageForm: db?.dosageForm || ex.dosageForm || 'Tablet',
    manufacturer: db?.manufacturer || '',
    category: db?.category || 'General',
    barcode: db?.barcode || null,
    purchasePricePaisa: toPaisa(db?.purchasePrice || 0),
    sellingPricePaisa: toPaisa(selling),
    mrpPaisa: toPaisa(selling),
    vatPercent: 13,
    reorderLevel: 10,
    requiresPrescription: false,
    scheduleCategory: 'OTC',
  };
}

const csvHeaders = ['brandName','genericName','composition','strength','dosageForm','manufacturer','category','barcode','purchasePricePaisa','sellingPricePaisa','mrpPaisa','vatPercent','reorderLevel','requiresPrescription','scheduleCategory'];
const csvRows = [csvHeaders.join(',')];
for (const ex of unmatchedExcel.slice(0, 100)) {
  const r = toImport(ex);
  csvRows.push(csvHeaders.map((h) => `"${String(r[h] ?? '').replace(/"/g, '""')}"`).join(','));
}
fs.writeFileSync(OUT_CSV, csvRows.join('\n'));

const matchTypes = matched.reduce((a, m) => { a[m.matchType] = (a[m.matchType] || 0) + 1; return a; }, {});

const report = {
  generatedAt: new Date().toISOString(),
  summary: {
    legacyDbMedicines: dbCount,
    excelProducts: excelRecords.length,
    excelMatchedInDb: matched.length,
    excelNotInDb: unmatchedExcel.length,
    excelMatchRate: `${((matched.length / excelRecords.length) * 100).toFixed(1)}%`,
    dbNotInExcelEstimate: dbCount - matched.length,
    excelCanEnrichDbPrices: priceEnrichment.length,
  },
  database: {
    path: DB_PATH,
    sizeMb: +(fs.statSync(DB_PATH).size / 1024 / 1024).toFixed(1),
    tables: sql('.tables').split(/\s+/).filter(Boolean),
    medicineColumns: ['id','brand_name','generic_name','category','strength','dosage_form','manufacturer','indications','side_effects','contraindications','storage','barcode','purchase_price','mrp','low_stock_threshold','is_favorite','last_viewed','stockQuantity','expiryDate','isActive'],
    stats: dbStats,
    samples: sqlRows('SELECT brand_name, generic_name, manufacturer, purchase_price, mrp, strength, dosage_form, barcode FROM medicines LIMIT 6'),
  },
  excel: {
    path: XLSX_PATH,
    sheetName,
    headers,
    columns: {
      Product_name: 'Medicine label (brand + strength + form combined)',
      Sales_price: 'Selling price NPR',
    },
    stats: {
      total: excelRecords.length,
      withPrice: excelRecords.filter((r) => r.salesPrice > 0).length,
      parsedStrength: excelRecords.filter((r) => r.strength).length,
      parsedForm: excelRecords.filter((r) => r.dosageForm).length,
      avgPrice: +(excelRecords.reduce((s, r) => s + r.salesPrice, 0) / excelRecords.length).toFixed(2),
    },
    parsedSamples: excelRecords.slice(0, 12),
  },
  comparison: { matchTypes, priceEnrichmentSample: priceEnrichment.slice(0, 20), excelNotInDbSample: unmatchedExcel.slice(0, 20), dbNotInExcelSample: unmatchedDb.slice(0, 20) },
  importFormat: { csvPath: OUT_CSV, headers: csvHeaders, unmatchedImportSamples: unmatchedExcel.slice(0, 8).map((e) => toImport(e)), enrichedSamples: priceEnrichment.slice(0, 5).map((p) => { const ex = excelRecords.find((e) => e.productName === p.excelProductName); return toImport(ex, { brandName: p.dbBrandName, genericName: p.genericName, manufacturer: p.manufacturer, strength: '', dosageForm: 'Tablet', category: 'General', barcode: null, purchasePrice: 0, mrp: 0 }); }) },
};

fs.writeFileSync(OUT_JSON, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report.summary, null, 2));
console.log('Match types:', matchTypes);
