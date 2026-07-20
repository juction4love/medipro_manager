# MediPro Firebase License Backend



## Architecture



```

App → Firebase Phone Auth (OTP) → Cloud Function → Firestore (licenses)

App → Firestore sync → pharmacies/{licenseId}/{collection}/{uuid}

Security Rules → tenant + device + syncVersion enforcement

```



**Client never writes to `licenses/` directly.**  

**271K master catalog (`catalog.db`) never syncs to Firestore.**



## Setup



1. Create Firebase project at https://console.firebase.google.com

2. Enable **Phone Authentication**

3. Add Android app → download `google-services.json` → place in `app/`

4. Apply Google Services plugin in `app/build.gradle.kts`:

   ```kotlin

   plugins { alias(libs.plugins.google.services) }

   ```

5. Set `USE_DEV_LICENSING = false` in release build

6. Update `LICENSE_API_BASE_URL` to your Cloud Functions URL

7. Edit `firebase/.firebaserc` with your project ID



## Deploy



```bash

cd firebase/functions

npm install



cd ..

npm install



# Rules + license functions

firebase deploy --only firestore:rules,functions

```



## Firestore Security Rules



See **[SECURITY-RULES.md](./SECURITY-RULES.md)** for the full v1.0 rule set:



| Rule | Summary |

|------|---------|

| Tenant isolation | `pharmacyUuid` must match licensed owner |

| Auth required | No anonymous access |

| Device validation | `deviceId` must match license |

| Soft delete only | `delete()` denied |

| Immutable fields | `uuid`, `createdAt`, `licenseId`, … |

| Sync version | Monotonic `syncVersion` required |

| Catalog blocked | No master catalog in Firestore |

| Branch ready | Optional `branchUuid` match |



### Test rules locally



```bash

firebase emulators:exec --only firestore "cd firebase && npm test"

```



## Firestore Collection: `licenses`



| Field | Example |

|-------|---------|

| licenseId | LIC-2026-00001 |

| pharmacyUuid | LIC-2026-00001 |

| firebaseUid | Firebase Auth UID |

| mobileNumber | 98XXXXXXXX |

| deviceId | abc123... |

| plan | FREE |

| status | ACTIVE |

| activationDate | 2026-07-19 |

| expiryDate | 2027-07-19 |

| branchUuid | null (future multi-branch) |



## Sync layout: `pharmacies/{licenseId}/…`



Subcollections: `invoices`, `purchases`, `sale_returns`, `purchase_returns`, `customers`, `suppliers`, `stock_adjustments`, `payments`, `ledger`, `audit_logs`, `medicines`, `stock_batches`, `settings`



## Cloud Function Endpoints



- `POST /createLicense` — after OTP verify, creates 1-year license + Auth custom claims

- `POST /verifyLicense` — periodic re-validation (14-day interval in app)

- `POST /transferLicense` — move license to new device (same mobile)



## Auth Custom Claims (set by Cloud Functions)



```json

{

  "licenseId": "LIC-2026-00001",

  "pharmacyUuid": "LIC-2026-00001",

  "deviceId": "registered-android-id",

  "plan": "FREE"

}

```



Re-login or token refresh required after license activation for claims to apply.

