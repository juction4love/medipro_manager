# Changelog

All notable changes to MediPro are documented here.

## [1.1.34] — 2026-07-20 — Internal Pilot Release

### Added
- Manual **batch picker** on sale (FEFO default; staff override for multi-batch medicines)
- **LOST** stock adjustment type
- In-app **Help / User Guide** (13 sections, offline search, contact support)
- Staff guide PDF/HTML in `docs/help/`
- Domain **SaleError** for posted-invoice cancellation messaging
- `CHANGELOG.md`, `SECURITY.md`, `CONTRIBUTING.md`

### Changed
- **Cancel Sale** disabled for posted invoices — use **Sales Return** workflow
- Sales Return remains the supported correction path (stock + ledger + audit)

### Fixed
- Production gate items for counter workflow stability
- Release signing and R8 build verified

### Security
- Keystore, `google-services.json` remain gitignored
- Large runtime `.db` assets removed from Git tracking

---

## Prior work (pre-tag baseline)

- Firebase Phone Auth licensing, Firestore sync, OCR purchase bills
- Room v15 migrations, encrypted backup, thermal printer
- Dashboard, accounting, day closing, inventory adjustments
