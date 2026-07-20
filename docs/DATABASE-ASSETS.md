# Database Assets (Not in Git)

Large SQLite files are **excluded from Git** to keep the repository lightweight and avoid GitHub size warnings.

| File | Size (approx) | Purpose |
|------|---------------|---------|
| `catalog.db` | ~88 MB | Read-only master medicine catalog (FTS search) |
| `seed.db` | ~3 MB | Optional first-launch inventory seed |
| `assets/medipro.db` | ~73 MB | Legacy source for ETL importer only |

## Build developers need

Before `assembleRelease`, ensure these exist:

```
app/src/main/assets/databases/catalog.db
app/src/main/assets/databases/seed.db   (optional)
```

## Option A — ETL pipeline (recommended)

```bash
cd assets/importer
npm install
npm run import
```

Then copy outputs:

```bash
cp assets/output/catalog.db app/src/main/assets/databases/catalog.db
cp assets/output/seed.db app/src/main/assets/databases/seed.db
```

See [assets/importer/README.md](../assets/importer/README.md).

## Option B — GitHub Release assets

Download prebuilt `catalog.db` / `seed.db` from the **v1.1.34** (or latest) GitHub Release and place them in `app/src/main/assets/databases/`.

## Runtime note

- **medipro.db** — created by Room at runtime (pharmacy data). Backed up via Settings → Backup.
- **catalog.db** — read-only asset; never synced to Firebase.
