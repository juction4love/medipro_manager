'use strict';

const path = require('path');
const { LegacyCatalogReader } = require('./LegacyCatalogReader');
const { ExcelReader } = require('./ExcelReader');
const { MedicineMatcher } = require('./MedicineMatcher');
const { MedicineEnricher } = require('./MedicineEnricher');
const { DuplicateResolver } = require('./DuplicateResolver');
const { RoomSeedWriter } = require('./RoomSeedWriter');
const { CatalogDbWriter } = require('./CatalogDbWriter');
const { ImportReport } = require('./ImportReport');

const ROOT = path.resolve(__dirname, '..');
const LEGACY_DB = path.join(ROOT, 'medipro.db');
const EXCEL = path.join(ROOT, 'product_list.xlsx');
const OUT_DIR = path.join(ROOT, 'output');
const SEED_DB = path.join(OUT_DIR, 'seed.db');
const CATALOG_DB = path.join(OUT_DIR, 'catalog.db');
const REPORT_JSON = path.join(OUT_DIR, 'import-report.json');

async function main() {
  console.log('MediPro ETL Import Pipeline');
  console.log('===========================\n');

  const report = new ImportReport();

  // Step 1 — Read Master Catalog
  console.log('Step 1: Reading legacy catalog...');
  const catalogReader = new LegacyCatalogReader(LEGACY_DB);
  const catalogInfo = catalogReader.read();
  report.stats.catalogTotal = catalogInfo.total;
  console.log(`  → ${catalogInfo.total.toLocaleString()} medicines indexed`);
  console.log(`  → Indexes:`, catalogInfo.indexes);

  // Step 2 — Read Excel
  console.log('\nStep 2: Reading Excel price list...');
  const excelReader = new ExcelReader(EXCEL);
  const excelInfo = excelReader.read();
  report.stats.excelTotal = excelInfo.total;
  console.log(`  → ${excelInfo.total.toLocaleString()} products (${excelInfo.withPrice} with price)`);

  // Steps 3–6 — Match, Enrich, Convert, Build entities
  console.log('\nSteps 3–6: Match → Enrich → Convert → Build entities...');
  const matcher = new MedicineMatcher();
  const enricher = new MedicineEnricher();
  const now = Date.now();
  const rawEntities = [];

  for (const excelRow of excelReader.records) {
    const match = matcher.match(excelRow, catalogReader);
    report.recordMatch(match.matchType);

    const enriched = enricher.enrich(excelRow, match, catalogReader);
    enriched.excelRowNum = excelRow.rowNum;

    if (match.matchType === 'missing') {
      report.recordMissing(excelRow);
    }

    const entity = DuplicateResolver.toMedicineEntity(enriched, excelRow.salesPrice, now);
    report.recordEntity(entity);
    rawEntities.push(entity);
  }

  // Step 7 — Enrichment replacements (Unknown stubs → catalog entries)
  console.log('\nStep 7: Catalog duplicate resolution (Unknown → enriched)...');
  const replacedUnknown = DuplicateResolver.countEnrichmentReplacements(rawEntities);
  report.stats.duplicatesRemoved = replacedUnknown;
  console.log(`  → ${replacedUnknown} Unknown stubs enriched from catalog`);

  const items = rawEntities;

  // Step 8 — Missing already tracked (47 expected)
  console.log(`\nStep 8: Missing medicines flagged: ${report.stats.missing}`);

  // Step 9 — Generate seed.db + catalog.db
  console.log('\nStep 9: Writing seed.db...');
  const seedWriter = new RoomSeedWriter(SEED_DB);
  const seedResult = seedWriter.write(items);
  console.log(`  → ${seedResult.path} (${seedResult.count} rows, ${seedResult.sizeMb} MB)`);

  console.log('\nStep 9b: Writing catalog.db (271K read-only)...');
  const catalogWriter = new CatalogDbWriter(CATALOG_DB);
  const catalogResult = catalogWriter.write(catalogReader.records);
  console.log(`  → ${catalogResult.path} (${catalogResult.count.toLocaleString()} rows, ${catalogResult.sizeMb} MB)`);

  // Step 10 — Import report
  console.log('\nStep 10: Generating import-report.json...');
  const json = report.finalize(seedResult, catalogResult);
  require('fs').writeFileSync(REPORT_JSON, JSON.stringify(json, null, 2));
  console.log(`  → ${REPORT_JSON}`);

  console.log('\n===========================');
  console.log('Import Summary');
  console.log('===========================');
  console.log(JSON.stringify(json.summary, null, 2));
  console.log('\nMatch types:', json.matchTypes);
}

main().catch((err) => {
  console.error('Import failed:', err);
  process.exit(1);
});
