# MediPro ETL Importer

Extract → Transform → Load pipeline for legacy pharmacy data.

## Inputs

| File | Description |
|------|-------------|
| `../medipro.db` | 271K master catalog (legacy schema) |
| `../product_list.xlsx` | 7,423 pharmacy products + sales prices |

## Outputs

| File | Description |
|------|-------------|
| `../output/seed.db` | Room v2 inventory DB (~7,423 medicines) |
| `../output/catalog.db` | Read-only master catalog (~271K, FTS search) |
| `../output/import-report.json` | Import statistics |

## Run

```bash
cd assets/importer
npm install
npm run import
```

Requires `sqlite3` (uses Android SDK platform-tools by default).

## Pipeline

```
LegacyCatalogReader → ExcelReader → MedicineMatcher → MedicineEnricher
  → PriceConverter → DuplicateResolver → RoomSeedWriter + CatalogDbWriter → ImportReport
```

## App integration

- `seed.db` → copied to `app/src/main/assets/databases/seed.db` → `SeedDataImporter` on first launch
- `catalog.db` → `app/src/main/assets/databases/catalog.db` → read-only offline master search

Re-run ETL after updating Excel or legacy DB, then copy outputs to app assets.
