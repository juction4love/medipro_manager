# MediPro Product Vision

**MediPro = Fast, Reliable Pharmacy ERP**

MediPro must let a pharmacy run **100–500 bills per day** without stock mismatch, crashes, or data loss.

> **Product principle:** Every medicine movement must be traceable, every stock value must reconcile, and every bill must complete quickly and correctly.

---

## Out of Scope (v1.x) ⭐⭐⭐⭐⭐

The following features are **intentionally excluded** until the core workflow  
**(Purchase → Sale → Stock → Reports → Backup)** is proven stable.

- CRM  
- Loyalty programs  
- AI recommendations  
- Advanced analytics  
- Multi-store  
- Online synchronization (beyond existing Firebase sync)  
- Accounting expansion  
- Distributor Portal / License v2  

**Rule:** No new feature unless it makes the core loop **more reliable**, not broader.

---

## Phase 1 — Must Be Perfect

These three modules must reach **production quality** before anything else expands.

### 1. Purchase ⭐⭐⭐⭐⭐

Purchase is the **source of inventory**.

```
Supplier
    ↓
Purchase Entry (Manual / OCR)
    ↓
Review
    ↓
Save
    ↓
Batch Created
    ↓
Stock Increased
```

**Must have (per line)**

- Medicine, batch number, expiry
- Qty, free qty, PTR, MRP, discount, VAT, amount
- Supplier, invoice number, invoice date
- Purchase return

**Never**

```
OCR → Auto Stock   ❌
```

**Always**

```
OCR → Review → Save   ✅
```

**Purchase validation (before save)**

| Check | Required |
|-------|----------|
| Supplier selected | ✓ |
| Invoice number | ✓ |
| Medicine exists | ✓ |
| Batch number | ✓ |
| Expiry valid | ✓ |
| MRP > PTR | ✓ |
| Qty > 0 | ✓ |
| Duplicate invoice? | ✓ (warn/block) |

**Purchase return flow**

```
Purchase → Purchase Return → Stock Reduce → Supplier Ledger → Accounting Entry
```

---

### 2. Sales (POS) ⭐⭐⭐⭐⭐

POS is the **main daily screen**.

**Target:** medicine search **< 100 ms**

```
Search / Barcode
    ↓
Select Batch
    ↓
Qty
    ↓
Payment
    ↓
Print
    ↓
Stock Reduce
```

**Sales validation (before save)** — any fail → **Cannot Complete Sale**

| Check | Required |
|-------|----------|
| Medicine active | ✓ |
| Stock available | ✓ |
| Batch selected | ✓ |
| Not expired | ✓ |
| MRP exists | ✓ |
| License valid | ✓ |
| Customer credit limit | ✓ (optional) |

**Sales return flow**

```
Sale → Sales Return → Stock Increase → Batch Restore → Accounting / Audit
```

---

### 3. Stock Adjustment ⭐⭐⭐⭐⭐

Inventory must always reconcile. Adjustments are **corrections only** — not a substitute for purchase.

**Two types**

| Increase | Decrease |
|----------|----------|
| Found stock | Broken, expired, lost, damage |

Every adjustment → **audit log**: medicine, batch, qty, reason, remarks, user, date/time. **Before / after / difference**. **No delete** — reverse only.

---

## Phase 1.5 — Inventory Verification (Stock Count) ⭐⭐⭐⭐

Every pharmacy performs physical counts weekly or monthly. This module **reduces manual adjustments**.

```
Stock Count Session
    ↓
Scan Shelf / Search Medicine
    ↓
Expected Qty (system)
    ↓
Actual Qty (counted)
    ↓
Difference
    ↓
Generate Adjustment (draft)
    ↓
Approval
    ↓
Stock Updated + Audit
```

**Target:** v1.2 (after core loop proven in pilot). Uses **Physical Count** adjustment type under the hood with structured workflow.

---

## Batch System

Stock must **never** exist only at medicine level.

```
Medicine → Batch → Quantity
```

**Sale:** FEFO — nearest expiry first, auto-suggest; staff may override (manual picker).

### Batch identity (production-critical)

Unique batch key:

```
MedicineId + BatchNumber + ExpiryDate
```

If the same batch number arrives with a **different expiry**, create a **separate batch row**.

**Database constraint (Room migration target)**

```sql
UNIQUE (medicineId, batchNumber, expiryDate)
```

Current code merges by `(medicineId, batchNumber)` only — **fix in v1.1.35**.

---

## Stock Model Rule

**Never**

```sql
UPDATE medicines SET stock = xxx   ❌
```

**Preferred**

```
medicine.totalStock = SUM(batch.quantity)   ✅
```

(or derive at query time from `stock` table joined to batches). Single source of truth prevents inconsistency between `batches.quantity` and `stock.quantity`.

---

## Price History

Every medicine view should show:

- Last purchase: supplier, PTR, MRP, date
- Average cost

---

## Dashboard (Simple)

Only:

- Today's sales
- Today's purchase
- Stock value
- Low stock
- Expiry (expired + near expiry)
- Receivable / payable

No widget overload.

---

## Reports (8 Essential)

1. Sales  
2. Purchase  
3. Inventory / stock  
4. Stock adjustment  
5. Expiry  
6. **Profit** — always `Sale Price − Purchase Cost` (never MRP-based profit)  
7. Supplier ledger  
8. Customer ledger  

---

## Backup

```
Backup → Verify (checksum) → Restore → Verify Again
```

- Encrypted `.medipro` (AES-GCM)
- Post-restore integrity check before app continues
- Checksum verification on backup file recommended (v1.1.36+)

---

## Architecture (Never Break)

```
UI → ViewModel → Repository → Room
```

- **Never** call DAO from UI directly.
- **Every** stock change inside a Room `@Transaction`.
- Stock changes **only** via: Purchase, Sale, Return, Adjustment, Stock Count (approved).

---

## Definition of Done

A feature is **complete** only if:

- [ ] Functional tests pass (manual or automated workflow)  
- [ ] Database integrity verified (no partial writes)  
- [ ] No crash during the full workflow  
- [ ] Audit logs generated where stock/accounting changes  
- [ ] Performance target achieved (see Performance Targets)  
- [ ] Unit tests updated (where applicable)  
- [ ] Documentation updated (`CHANGELOG`, Help, or this doc if scope changes)  

---

## Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| **Availability** | Offline-first — counter works without network |
| **Performance** | Search < 100 ms; sale save < 1 s (see targets) |
| **Reliability** | No stock inconsistency; formula reconciles |
| **Security** | Encrypted backup; signed release only; no secrets in Git |
| **Maintainability** | Clean Architecture; UI → ViewModel → Repository → Room |

---

## Database Principles

- **No destructive migration** in production  
- Every schema change uses **Room Migration** + exported schema  
- **Foreign keys** enabled where applicable  
- **Transactions** for all inventory changes  
- Seed / catalog databases are **read-only** assets  
- Batch uniqueness: `(medicineId, batchNumber, expiryDate)` (v1.1.35)  

---

## Error Recovery

| Failure | Expected behaviour |
|---------|-------------------|
| Purchase fails | **No** partial stock update (transaction rollback) |
| Sale fails | **No** stock deduction |
| Restore fails | **Keep** existing database; show error |
| OCR fails | Manual purchase entry always available |
| Sync fails | Local data intact; queue retries |

User must always have a path to complete work without data loss.

---

| # | Rule |
|---|------|
| 1 | No purchase → no stock |
| 2 | No stock → no sale |
| 3 | No expired medicine sale |
| 4 | Every stock change logged |
| 5 | Delete never — return / reverse only |
| 6 | Every transaction inside Room transaction |
| 7 | OCR never changes database directly |
| 8 | Barcode scanner ≠ invoice OCR |
| 9 | Stock formula must reconcile (below) |
| 10 | User never edits stock directly |
| 11 | Medicine stock = sum of batch stock (never manual medicine-level override) |

**Stock reconciliation formula**

```
Stock = Purchase
      + Adjustment Increase
      - Sales
      - Purchase Return
      - Adjustment Decrease
      + Sales Return
```

If this does not match → **bug**.

---

## Performance Targets

| Operation | Target |
|-----------|--------|
| App launch | < 2 s |
| Medicine search | < 100 ms |
| Barcode scan → add | < 300 ms |
| Purchase save | < 2 s |
| Sale save | < 1 s |
| Dashboard load | < 1 s |

No ANR. No crashes in pilot.

---

## Security (Release)

| Control | Required |
|---------|----------|
| Encrypted backup | ✓ |
| Room SQL transactions | ✓ |
| Audit log | ✓ |
| Crashlytics | ✓ |
| No debug logs in release | ✓ |
| License gate | ✓ |

---

## Success Metrics (Pilot & Production)

| Metric | Target |
|--------|--------|
| Crash-free sessions | > 99.9% |
| Search latency | < 100 ms |
| Bill completion (search → print) | < 30 sec |
| Stock mismatch | 0 |
| Data loss | 0 |
| Inventory reconciliation | 100% |
| Report match (sales / stock / profit) | 100% |

**Pilot week gate:** 100 bills with all metrics above.

---

## Versioning Policy

| Series | Purpose |
|--------|---------|
| **v1.1.x** | Stability & bug fixes (pilot → production hardening) |
| **v1.2.x** | Operational improvements (stock count, perf, reports) |
| **v2.x** | New platform features (signed licensing, distributor, multi-store) |

Patch (`x`) = fixes only. Minor = approved roadmap items only. No scope creep in patch releases.

---

## Release Gate

**Release is blocked if any of the following fail:**

- Any stock mismatch exists in QA  
- Purchase cannot post to stock  
- Sale cannot complete end-to-end  
- Backup cannot restore verified data  
- Reports do not reconcile with stock  
- Critical crash exists on pilot devices  

All **Definition of Done** items must pass for changed modules.

---

## Development Roadmap

### v1.1.35 — Core hardening

- Purchase validation (full checklist)
- Sale validation (full checklist)
- Hold / resume bill (POS)
- Batch identity: `(medicineId, batchNumber, expiryDate)` + UNIQUE
- Stock bug fixes (purchase save, stock 0, cart add)
- Dual-qty reconciliation (`stock` vs `batch`)

### v1.1.36 — Visibility & perf

- Price history UI
- Dashboard simplification
- Essential 8 reports trim + profit = sale − cost
- Performance optimization vs targets
- Backup checksum verify

### v1.2 — Scale

- Distributor licensing v2
- **Inventory stock count** (verification module)
- Multi-user permissions
- Multi-store (if required)

---

## Before Public Release (Play Store / Commercial)

### Functional

- [ ] Purchase  
- [ ] Purchase return  
- [ ] Sales  
- [ ] Sales return  
- [ ] Stock adjustment  
- [ ] Backup / restore  

### Data integrity

- [ ] No negative stock  
- [ ] FEFO respected  
- [ ] Batch integrity (unique key enforced)  
- [ ] Correct reports  
- [ ] Correct stock valuation  
- [ ] Stock formula reconciles  

### Performance

- [ ] All targets in table above  

---

## Core Loop (Daily Use)

```
Purchase → Inventory Updated → Sale → Stock Reduced
    → Adjustment / Stock Count (if needed) → Reports → Backup
```

If this loop is **fast, correct, and crash-free**, MediPro is commercially viable.

---

## Critical Warning — Core Workflow Bugs

Do **not** add analytics, advanced dashboard, or CRM until **proven stable on device**:

- Purchase save failing  
- Stock remaining 0 after purchase  
- Sale bill not adding items  

Fix **Purchase → Stock → Sale** before any new features.

---

## v1.1.34 Pilot Status (Snapshot)

| Area | Status |
|------|--------|
| Purchase + OCR review path | Pilot — validate on device |
| Sales POS + FEFO + batch picker | Pilot — validate on device |
| Stock adjustment + audit | Implemented |
| Cancel sale disabled → return only | Implemented |
| Purchase / sale validation (full) | Partial |
| Batch unique (number + expiry) | Not yet — v1.1.35 |
| Stock count module | Planned v1.2 |
| Price history UI | Partial |
| Dashboard / reports trim | v1.1.36 |
| Play Store compliance | Not ready |

---

> **Guiding principle:** Every purchase must increase stock correctly, every sale must decrease stock correctly, and every inventory movement must be traceable, auditable, and recoverable.

*Document owner: Bimal Tech Solution · Locked for v1.1.x development · Production planning baseline: 10/10*
