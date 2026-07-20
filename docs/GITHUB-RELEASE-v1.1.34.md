# MediPro v1.1.34 – Internal Pilot Release

**Tag:** `v1.1.34`  
**Date:** 2026-07-20

## Highlights

### Features
- **Manual Batch Picker** on sale — FEFO default, staff override when multiple batches exist
- **LOST** stock adjustment type for physical verification gaps
- **Sales Return** workflow — correct way to reverse posted invoices (cancel disabled)
- **In-app Help** — 13 chapters, offline search, contact support
- **Staff Guide** — PDF/HTML in `docs/help/`

### Improvements
- Production gate stability fixes
- Typed domain errors for sale operations (`SaleError`)
- Project docs: CHANGELOG, SECURITY, CONTRIBUTING, LICENSE

### Build notes
- Large database files removed from Git — download **catalog.db** / **seed.db** below or run ETL ([docs/DATABASE-ASSETS.md](docs/DATABASE-ASSETS.md))
- Requires `google-services.json` and keystore locally (not in repo)

## Recommended attachments for this release

| File | Purpose |
|------|---------|
| `app-release.apk` | Signed pilot APK (~98 MB) |
| `catalog.db` | Build asset for medicine catalog |
| `seed.db` | Optional first-launch seed |
| `MediPro-Staff-Guide-v1.1.34.pdf` | Staff training |

## Create on GitHub (manual)

1. https://github.com/juction4love/medipro_manager/releases/new
2. Choose tag **v1.1.34**
3. Title: **MediPro v1.1.34 – Internal Pilot Release**
4. Paste this file as release notes
5. Upload APK + database assets + PDF

## Pilot checklist

- [ ] Install signed APK on device
- [ ] OTP activation test
- [ ] Purchase → Sale → Return → Adjustment → Backup
- [ ] Enable Crashlytics monitoring
- [ ] Collect staff feedback for v1.1.35
