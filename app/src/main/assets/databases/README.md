# App database assets

This folder must contain `catalog.db` (and optionally `seed.db`) **before building a release APK**.

These files are **not stored in Git** (too large). See:

- [docs/DATABASE-ASSETS.md](../../../../docs/DATABASE-ASSETS.md)
- [assets/importer/README.md](../../../../assets/importer/README.md)

Quick setup after clone:

```bash
cd assets/importer && npm install && npm run import
cp ../output/catalog.db ../../app/src/main/assets/databases/
cp ../output/seed.db ../../app/src/main/assets/databases/
```
