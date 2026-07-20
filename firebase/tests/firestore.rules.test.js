/**
 * MediPro Firestore Security Rules — unit tests
 *
 * Run: cd firebase && npm install && npm test
 *
 * Requires Firebase Emulator or set FIRESTORE_EMULATOR_HOST.
 */
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const test = require('node:test');
const assert = require('node:assert/strict');

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require('@firebase/rules-unit-testing');

const PROJECT_ID = 'medipro-rules-test';
const PHARMACY = 'LIC-2026-00001';
const DEVICE = 'device-abc';
const OTHER_DEVICE = 'device-xyz';
const UID = 'user-uid-1';
const OTHER_UID = 'user-uid-2';

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection('licenses').doc(PHARMACY).set({
      licenseId: PHARMACY,
      pharmacyUuid: PHARMACY,
      firebaseUid: UID,
      deviceId: DEVICE,
      status: 'ACTIVE',
      expiryEpochMs: Date.now() + 86_400_000,
      plan: 'FREE',
    });
    await adminDb
      .collection('pharmacies')
      .doc(PHARMACY)
      .collection('invoices')
      .doc('inv-1')
      .set({
        uuid: 'inv-1',
        entityType: 'INVOICE',
        licenseId: PHARMACY,
        createdAt: 1,
        updatedAt: 1,
        syncVersion: 1,
        deviceId: DEVICE,
        payload: '{}',
      });
  });
});

test.after(async () => {
  await testEnv.cleanup();
});

function authedDb(uid = UID, claims = { deviceId: DEVICE }) {
  return testEnv.authenticatedContext(uid, claims).firestore();
}

function baseInvoice(overrides = {}) {
  return {
    uuid: 'inv-new',
    entityType: 'INVOICE',
    licenseId: PHARMACY,
    createdAt: 100,
    updatedAt: 100,
    syncVersion: 1,
    deviceId: DEVICE,
    payload: '{}',
    ...overrides,
  };
}

test('Rule 2 — unauthenticated read denied', async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').get(),
  );
});

test('Rule 1 — wrong tenant uid denied', async () => {
  const db = authedDb(OTHER_UID);
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').get(),
  );
});

test('Rule 1 — owner can read own pharmacy data', async () => {
  const db = authedDb();
  await assertSucceeds(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').get(),
  );
});

test('Rule 3 — wrong deviceId on create denied', async () => {
  const db = authedDb();
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-bad').set(
      baseInvoice({ uuid: 'inv-bad', deviceId: OTHER_DEVICE }),
    ),
  );
});

test('Rule 6 — stale syncVersion update denied', async () => {
  const db = authedDb();
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').set(
      {
        uuid: 'inv-1',
        entityType: 'INVOICE',
        licenseId: PHARMACY,
        createdAt: 1,
        updatedAt: 2,
        syncVersion: 1,
        deviceId: DEVICE,
        payload: '{}',
      },
      { merge: true },
    ),
  );
});

test('Rule 6 — higher syncVersion update allowed', async () => {
  const db = authedDb();
  await assertSucceeds(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').set(
      {
        uuid: 'inv-1',
        entityType: 'INVOICE',
        licenseId: PHARMACY,
        createdAt: 1,
        updatedAt: 2,
        syncVersion: 2,
        deviceId: DEVICE,
        payload: '{"updated":true}',
      },
      { merge: true },
    ),
  );
});

test('Rule 4 — physical delete denied', async () => {
  const db = authedDb();
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-1').delete(),
  );
});

test('Rule 7 — catalog upload denied', async () => {
  const db = authedDb();
  await assertFails(
    db.collection('catalog').doc('medicines').set({ count: 271000 }),
  );
});

test('Rule 5 — uuid document id must match', async () => {
  const db = authedDb();
  await assertFails(
    db.collection('pharmacies').doc(PHARMACY).collection('invoices').doc('inv-2').set(
      baseInvoice({ uuid: 'different-uuid' }),
    ),
  );
});
