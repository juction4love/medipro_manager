# MediPro ERP — Pharmacy Management (2026)

Offline-first Pharmacy ERP for Nepal built with **Kotlin 2.x**, **Jetpack Compose**, **Clean Architecture + MVVM**, **Hilt**, and **Room**.

## Architecture

```
UI (Compose) → ViewModel → UseCase → Repository → Room → SQLite
```

Future cloud sync layer:

```
Room → Sync Queue → WorkManager → Firebase → Cloud DB → Second Device
```

## Project Structure

| Module | Purpose |
|--------|---------|
| `:app` | Application, navigation, splash, app lock |
| `:core:common` | Shared utilities, BaseViewModel, Result |
| `:core:database` | Room entities, DAOs, DB v1 |
| `:core:designsystem` | Theme, reusable UI components |
| `:core:security` | Keystore encryption, PIN/biometric lock |
| `:core:datastore` | App preferences |
| `:core:worker` | WorkManager (auto backup) |
| `:domain` | Models, repository interfaces, use cases |
| `:data` | Repository implementations, mappers |
| `:feature:*` | Feature modules (dashboard, medicine, sales, …) |

## Database (Version 1)

Entities: Medicine, Supplier, Customer, Purchase, PurchaseItem, Sale, SaleItem, Stock, Batch, Return, Expense, Income, Ledger, Payment, BackupHistory, License, Settings

## Navigation Flow

```
Splash → License → App Lock → Dashboard → Features
```

## Implementation Roadmap

| Priority | Module | Status |
|----------|--------|--------|
| 🟢 1 | Sales POS | ✅ v1 done |
| 🟢 2 | Purchase | Next |
| 🟢 3 | Inventory | Planned |
| 🟢 4 | Reports | Planned |
| 🟡 5 | Encrypted Backup | Phase 2 |
| 🟡 6 | Purchase OCR | Phase 2 |
| 🟡 7 | Firebase | Phase 2 |
| 🔵 8 | Cloud Sync | Phase 3 |

## v1.0 Features

- Dashboard with today's stats
- Medicine CRUD + search
- **Sales POS** — search, barcode, cart, VAT, cash/credit, stock deduction, PDF invoice
- License verification
- App lock (PIN)
- Settings (pharmacy profile)
- Scaffold modules: Purchase, Inventory, Reports, Accounting, Backup, Scanner, …

## Requirements

- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35
- Min SDK 26

## Build

```bash
./gradlew assembleDebug
```

Open `G:\medipro` in Android Studio and sync Gradle.

## Package Name

`com.medipro.manager`

## Notes

- **Offline-first**: All data stored locally in SQLite via Room.
- **Firebase** (Crashlytics/Analytics): Add `google-services.json` to `:app` when ready.
- **Database migrations**: Increment version in `MediProDatabase` only when schema changes (1 → 2 → 3 …).
