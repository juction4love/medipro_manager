# MediPro Firestore Security Rules v1.0

Production security layer for offline-first pharmacy sync. Deploy **before** enabling cloud sync in production.

## Architecture

```
App (Firebase Phone Auth)
  → Cloud Functions (license create/verify/transfer)
  → Firestore licenses/{licenseId}
  → App sync writes pharmacies/{licenseId}/{collection}/{uuid}
  → Security Rules enforce tenant + device + version
```

**271K master catalog (`catalog.db`) is NEVER uploaded** — rules deny `catalog/*`, `master_catalog/*`.

## Collections

| Path | Purpose |
|------|---------|
| `licenses/{licenseId}` | Tenant root — Cloud Functions only write |
| `pharmacies/{pharmacyUuid}/invoices/{uuid}` | Sales |
| `pharmacies/{pharmacyUuid}/purchases/{uuid}` | Purchases |
| `pharmacies/{pharmacyUuid}/sale_returns/{uuid}` | Sales returns |
| `pharmacies/{pharmacyUuid}/purchase_returns/{uuid}` | Purchase returns |
| `pharmacies/{pharmacyUuid}/customers/{uuid}` | Customers |
| `pharmacies/{pharmacyUuid}/suppliers/{uuid}` | Suppliers |
| `pharmacies/{pharmacyUuid}/stock_adjustments/{uuid}` | Stock adjustments |
| `pharmacies/{pharmacyUuid}/payments/{uuid}` | Payments |
| `pharmacies/{pharmacyUuid}/ledger/{uuid}` | Ledger entries |
| `pharmacies/{pharmacyUuid}/audit_logs/{uuid}` | Audit trail |
| `pharmacies/{pharmacyUuid}/medicines/{uuid}` | Pharmacy-owned inventory meds (NOT 271K catalog) |
| `pharmacies/{pharmacyUuid}/stock_batches/{uuid}` | Batch stock |
| `pharmacies/{pharmacyUuid}/settings/{uuid}` | Settings |

In MediPro Android, `pharmacyUuid` == `licenseId` from the activated license.

## Eight Security Rules

| # | Rule | Implementation |
|---|------|----------------|
| 1 | **Tenant isolation** | `licenses/{pharmacyUuid}` must exist; `firebaseUid == request.auth.uid`; `licenseId/pharmacyUuid` match path |
| 2 | **Auth required** | All pharmacy reads/writes require `request.auth != null` |
| 3 | **Device validation** | `request.resource.data.deviceId` must match license `deviceId`; optional Auth custom claim `deviceId` |
| 4 | **Soft delete only** | `allow delete: if false` — use `deletedAt` field update |
| 5 | **Immutable fields** | `uuid`, `createdAt`, `entityType`, `licenseId`, `createdBy` cannot change on update |
| 6 | **Sync version** | Create: `syncVersion >= 1`; Update: incoming `syncVersion >` cloud `syncVersion` |
| 7 | **Catalog never sync** | Deny all `catalog/*`, `master_catalog/*` paths |
| 8 | **Branch ready** | If license has `branchUuid`, document `branchUuid` must match or be null |

## Deploy

1. Edit `.firebaserc` — set your Firebase project ID.
2. Install Firebase CLI: `npm install -g firebase-tools`
3. Login: `firebase login`
4. Deploy rules + functions:

```bash
cd firebase
firebase deploy --only firestore:rules,functions
```

Rules only:

```bash
firebase deploy --only firestore:rules
```

## License Custom Claims

Cloud Functions set Auth custom claims on create/verify/transfer:

```json
{
  "licenseId": "LIC-2026-00001",
  "pharmacyUuid": "LIC-2026-00001",
  "deviceId": "android-device-id",
  "plan": "FREE"
}
```

Rules validate `deviceId` on writes against the license document (primary) and claims (secondary).

## Client Sync Document Shape

Each synced document must include:

```json
{
  "uuid": "same-as-document-id",
  "entityType": "INVOICE",
  "licenseId": "LIC-2026-00001",
  "createdAt": 1710000000000,
  "updatedAt": 1710000001000,
  "syncVersion": 2,
  "deletedAt": null,
  "deviceId": "registered-device-id",
  "payload": "{...}"
}
```

## Testing Rules Locally

```bash
cd firebase
npm install
npm test
```

Uses `@firebase/rules-unit-testing` against `firestore.rules`.

## Production Checklist

- [ ] `.firebaserc` project ID set
- [ ] Phone Auth enabled in Firebase Console
- [ ] `firestore.rules` deployed
- [ ] Cloud Functions deployed (license + custom claims)
- [ ] App `USE_DEV_LICENSING = false` in release
- [ ] `google-services.json` in `app/`
- [ ] Verify: unauthenticated client cannot read/write pharmacy data
- [ ] Verify: wrong `pharmacyUuid` path denied
- [ ] Verify: wrong `deviceId` write denied
- [ ] Verify: stale `syncVersion` update denied
- [ ] Verify: physical `delete()` denied
