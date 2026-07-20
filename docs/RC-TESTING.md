# MediPro ERP — RC Testing Checklist (Production Gate)

**Version:** 1.0  
**Target:** Release Candidate sign-off before Play Store / pharmacy rollout  
**DB version:** Room v10 | **Catalog:** 271K (read-only `catalog.db`)

---

## Gate status summary

| Area | Priority | Automated | Manual | RC status |
|------|----------|-----------|--------|-----------|
| 1. Database & Migration | ⭐⭐⭐⭐⭐ | Partial | Required | **BLOCKED** |
| 2. Performance | ⭐⭐⭐⭐⭐ | Partial | Required | Pending |
| 3. Multi-device Sync | ⭐⭐⭐⭐⭐ | Partial | Required | Pending |
| 4. Accounting Verification | ⭐⭐⭐⭐⭐ | None | Required | Pending |
| 5. Inventory Validation | ⭐⭐⭐⭐⭐ | None | Required | Pending |
| 6. Security | ⭐⭐⭐⭐⭐ | Partial | Required | Pending |
| 7. Stress Test | ⭐⭐⭐⭐ | None | Required | Pending |
| 8. Crash Testing | ⭐⭐⭐⭐ | None | Required | Pending |

**Run automatable checks:**

```powershell
.\scripts\rc-run-gate.ps1
```

---

## Critical blockers (must fix before RC)

### B1 — Destructive migrations **FIXED (P0 Phase 1)**

`DatabaseMigrations.kt` implements `MIGRATION_1_2` … `MIGRATION_9_10`.  
`DataModule.kt` uses `.addMigrations(*DatabaseMigrations.ALL)` — **`fallbackToDestructiveMigration()` removed**.

Verify: `./gradlew :core:database:connectedDebugAndroidTest` (device/emulator)

**RC requirement:** Implement incremental migrations v1→v10 (or staged path) + `MigrationTestHelper` tests. Until then, Item 1 cannot pass.

### B2 — Backup / restore not implemented (Item 1 fail)

`BackupRepository` interface exists; `BackupRepositoryImpl` and real DB export/import are **not implemented**. `AutoBackupWorker` and `BackupScreen` are stubs.

**RC requirement:** Encrypted backup round-trip before production gate.

---

## 1. Database & Migration ⭐⭐⭐⭐⭐

### Goals (Nepali summary)
- सबै migrations (v1 → v10) test गर्नुहोस्
- पुरानो database बाट data loss नभएको verify गर्नुहोस्
- Backup → Restore → App restart → data integrity check

### Test matrix

| # | Scenario | Steps | Pass criteria | Status |
|---|----------|-------|---------------|--------|
| 1.1 | Fresh install v10 | Install APK on clean device | App opens; seed/import works | Manual |
| 1.2 | Upgrade v9 → v10 | Install old APK with data → update to RC | **All rows preserved**; no wipe | **BLOCKED** (destructive) |
| 1.3 | Upgrade v1 → v10 | Legacy schema device → RC | Migration path or forced backup prompt | **BLOCKED** |
| 1.4 | Schema export drift | `./gradlew :core:database:compileDebugKotlin` | Exported JSON matches entity definitions | Manual |
| 1.5 | Backup round-trip | Create sale → backup → wipe → restore → restart | Invoice, stock, ledger match | **BLOCKED** (no impl) |
| 1.6 | Post-restart integrity | After 1.5, open Dashboard, Reports, POS | Counts and balances unchanged | Manual |

### Key files
- `core/database/MediProDatabase.kt` (version 10)
- `core/database/schemas/.../1.json` … `10.json`
- `data/di/DataModule.kt` — migration strategy
- `core/security/KeystoreManager.kt` — backup encryption (ready)

### Automation target
```bash
./gradlew :core:database:connectedAndroidTest  # after MigrationTestHelper added
```

---

## 2. Performance ⭐⭐⭐⭐⭐

### Targets (271K master catalog)

| Metric | Target | How to measure |
|--------|--------|----------------|
| Brand search | < 50 ms | POS search, brand prefix (e.g. "Para") |
| Generic search | < 50 ms | Generic/composition query |
| Barcode lookup | < 20 ms | Scan or exact barcode |
| Cold start | < 3 s | Mid-range device, release build |
| Dashboard first content | < 2 s | Time to KPI cards visible |

### Test matrix

| # | Scenario | Pass criteria |
|---|----------|---------------|
| 2.1 | Catalog brand FTS | p95 < 50 ms over 20 queries |
| 2.2 | Catalog generic FTS | p95 < 50 ms |
| 2.3 | Barcode exact match | p95 < 20 ms |
| 2.4 | Inventory + catalog merge | `PosSearchRepositoryImpl` returns in < 80 ms |
| 2.5 | Cold start (release) | Splash → Dashboard < 3 s |
| 2.6 | Dashboard load | Summary cards < 2 s |
| 2.7 | Reports tab switch | Lazy tab load < 1 s each |

### Key files
- `data/repository/PosSearchRepositoryImpl.kt`
- `data/catalog/CatalogDatabaseHelper.kt`
- `app/src/main/assets/databases/catalog.db` (~271K rows)
- `assets/output/import-report.json` — expected count 271,044

### Measurement
- Android Studio Profiler (CPU + SQL)
- `adb shell am start -W` for cold start
- Optional: Macrobenchmark module (not yet added)

```powershell
# Catalog row-count integrity (requires sqlite3)
.\scripts\rc-catalog-integrity.ps1
```

---

## 3. Multi-device Sync ⭐⭐⭐⭐⭐

**Minimum:** 2 Android devices, same `licenseId`, different `deviceId`.

### Test matrix

| # | Scenario | Device A | Device B | Pass criteria |
|---|----------|----------|----------|---------------|
| 3.1 | Sale sync | Create sale | Listener within 30 s | Invoice + stock qty match |
| 3.2 | Purchase sync | Create purchase | Listener | Stock batch qty increased |
| 3.3 | Customer edit conflict | Edit customer name | Edit same customer offline | LWW: higher `syncVersion` wins |
| 3.4 | Settings update | Change pharmacy name | — | Settings reflect on B |
| 3.5 | Offline → online | Queue ops offline | — | All pending sync; no duplicates |
| 3.6 | Simultaneous edits | Edit medicine price | Edit reorder level | Conflict resolved; no crash |
| 3.7 | Stock adjustment | Damage adjustment | — | Batch qty + adjustment row on B |
| 3.8 | Audit log | Any audited action | — | Audit row appears on B (append-only) |

### Conflict rule (verified in unit tests)
`syncVersion` → `updatedAt` → ignore same `deviceId` echo.

**Automated:** `data/src/test/.../SyncConflictResolverTest.kt`  
**Manual:** Required for full gate.

### Key files
- `data/sync/FirestoreSyncRepositoryImpl.kt` — 11 listener collections
- `data/sync/FirestoreRemoteApplier.kt`
- `data/sync/SyncPushWorker.kt`

---

## 4. Accounting Verification ⭐⭐⭐⭐⭐

After **each** workflow below, verify all columns reconcile:

| Check | Source |
|-------|--------|
| Ledger | `LedgerDao` / Accounting tab |
| Customer Due | Customer outstanding balance |
| Supplier Due | Supplier outstanding balance |
| Cash Drawer | Cash ledger + payments |
| Inventory Value | `StockDao.getInventoryValue()` |
| Profit | Sales − COGS (Reports Financial tab) |

### Workflows to test

| # | Workflow | Verify |
|---|----------|--------|
| 4.1 | Cash sale | Ledger credit SALES; cash debit; stock reduced |
| 4.2 | Credit sale | Customer due increased |
| 4.3 | Cash purchase | Supplier paid; stock increased |
| 4.4 | Credit purchase | Supplier due increased |
| 4.5 | Customer payment | Due decreased; cash increased |
| 4.6 | Supplier payment | Due decreased |
| 4.7 | Sale return | Stock restored; ledger reversed |
| 4.8 | Purchase return | Stock reduced; supplier credit |
| 4.9 | Stock adjustment | Inventory value matches batch sum |

**Pass:** Manual spreadsheet or Reports export — all totals match within ₹0.01.

---

## 5. Inventory Validation ⭐⭐⭐⭐⭐

| # | Scenario | Pass criteria |
|---|----------|---------------|
| 5.1 | FEFO batch selection | POS picks earliest non-expired batch |
| 5.2 | Expired batch sale blocked | Cannot sell expired batch |
| 5.3 | Damage adjustment | Sellable ↓, damaged ↑, syncs |
| 5.4 | Physical count | Set exact qty; audit log created |
| 5.5 | Purchase return | Qty restored correctly |
| 5.6 | Sales return | Qty returned to batch |
| 5.7 | Multi-batch sale | FEFO splits across batches |

### Key files
- `feature/sales/` — FEFO in ViewModel
- `data/repository/InventoryRepositoryImpl.kt`
- `data/repository/SaleReturnRepositoryImpl.kt`
- `data/repository/PurchaseReturnRepositoryImpl.kt`

---

## 6. Security ⭐⭐⭐⭐⭐

| # | Scenario | Pass criteria | Automated |
|---|----------|---------------|-----------|
| 6.1 | OTP activation | Real Firebase OTP (release build) | Manual |
| 6.2 | Device binding | Second device rejected or transfer flow | Manual |
| 6.3 | Expired license | App blocks; grace message shown | Manual |
| 6.4 | Offline grace | Works N days offline per policy | Manual |
| 6.5 | Firestore rules | Emulator tests pass | **Yes** |
| 6.6 | Unauthorized tenant | Cross-pharmacy read/write fails | **Yes** |
| 6.7 | Catalog write blocked | Client cannot write `catalog` collection | **Yes** |
| 6.8 | App lock / biometric | PIN required after background | Manual |

```bash
cd firebase
firebase emulators:exec --only firestore "npm test"
```

**Note:** Debug builds use `USE_DEV_LICENSING=true` — security gate must use **release** build.

### Key files
- `firebase/firestore.rules`
- `firebase/tests/firestore.rules.test.js`
- `feature/license/` — OTP flow
- `core/security/AppLockManager.kt`

---

## 7. Stress Test ⭐⭐⭐⭐

Simulate high volume (seed script or bulk import):

| Dataset | Target count | Pass criteria |
|---------|--------------|---------------|
| Invoices | 100,000 | List scroll 60 fps; search < 2 s |
| Customers | 20,000 | Search + sync queue stable |
| Suppliers | 10,000 | Same |
| Catalog | 271K | Search targets in §2 |

| # | Check | Pass |
|---|-------|------|
| 7.1 | POS search under load | UI responsive |
| 7.2 | Sync queue 500+ pending | Worker drains; no OOM |
| 7.3 | Reports export 10K rows | PDF/CSV completes |
| 7.4 | Dashboard with large DB | Opens < 5 s |

**Status:** No seed script in repo yet — create `scripts/seed-stress-data.kt` or manual bulk import.

---

## 8. Crash Testing ⭐⭐⭐⭐

| # | Scenario | Pass criteria |
|---|----------|---------------|
| 8.1 | Kill app during sync | Restart → pending ops retry; no corrupt DB |
| 8.2 | Network disconnect mid-push | Ops stay PENDING; resume on reconnect |
| 8.3 | Power loss during purchase | Transaction rolled back OR consistent state |
| 8.4 | Force close during backup | No partial corrupt backup file |
| 8.5 | Low storage (< 100 MB) | Graceful error; no silent data loss |

Room uses transactions for purchase/sale — verify WAL integrity after force-stop.

---

## Sign-off template

| Role | Name | Date | Pass / Fail |
|------|------|------|-------------|
| QA Lead | | | |
| Dev Lead | | | |
| Product Owner | | | |

**RC Approved:** ☐ Yes ☐ No — Blockers: _______________

---

## Automated gate commands

| Command | Covers |
|---------|--------|
| `.\scripts\rc-run-gate.ps1` | Unit tests + catalog integrity + instructions for Firestore |
| `./gradlew :data:testDebugUnitTest` | SyncConflictResolver |
| `firebase emulators:exec --only firestore "cd firebase && npm test"` | Security rules |
| `./gradlew :app:assembleRelease` | Release build smoke |

---

## Recommended fix order before RC

1. **P0** — Room migrations v1→v10 + migration tests  
2. **P0** — BackupRepositoryImpl (encrypted export/import)  
3. **P1** — Performance baseline script + Macrobenchmark  
4. **P1** — Multi-device sync QA matrix (§3)  
5. **P2** — Stress seed script  
6. **P2** — CI workflow for rules + unit tests  
