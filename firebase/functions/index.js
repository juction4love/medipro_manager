/**
 * MediPro License Cloud Functions
 *
 * Deploy: firebase deploy --only functions
 * Security: Client NEVER writes to Firestore directly.
 */
const { onRequest, HttpsError } = require('firebase-functions/v2/https');
const { setGlobalOptions } = require('firebase-functions/v2');
const admin = require('firebase-admin');

setGlobalOptions({ region: 'us-central1' });

const ONE_YEAR_MS = 365 * 24 * 60 * 60 * 1000;

function getDb() {
  if (!admin.apps.length) {
    admin.initializeApp();
  }
  return admin.firestore();
}

async function verifyFirebaseToken(idToken) {
  if (!idToken) {
    throw new HttpsError('unauthenticated', 'Missing idToken');
  }
  return admin.auth().verifyIdToken(idToken);
}

async function syncAuthClaims(uid, license) {
  const pharmacyUuid = license.pharmacyUuid || license.licenseId;
  await admin.auth().setCustomUserClaims(uid, {
    licenseId: license.licenseId,
    pharmacyUuid,
    deviceId: license.deviceId,
    plan: license.plan || 'FREE',
  });
}

function formatLicenseId() {
  const year = new Date().getFullYear();
  const seq = Math.floor(Math.random() * 99999).toString().padStart(5, '0');
  return `LIC-${year}-${seq}`;
}

exports.createLicense = onRequest(async (req, res) => {
  try {
    if (req.method !== 'POST') return res.status(405).send('Method not allowed');
    const { idToken, mobileNumber, deviceId, pharmacyName, ownerName } = req.body;
    const decoded = await verifyFirebaseToken(idToken);
    const db = getDb();

    const existing = await db.collection('licenses')
      .where('mobileNumber', '==', mobileNumber)
      .limit(1)
      .get();

    if (!existing.empty) {
      const doc = existing.docs[0].data();
      if (doc.deviceId !== deviceId) {
        return res.status(409).json({
          error: 'DEVICE_MISMATCH',
          message: 'License bound to another device. Transfer required.',
          licenseId: doc.licenseId,
        });
      }
      await syncAuthClaims(decoded.uid, doc);
      return res.json(toApiResponse(doc));
    }

    const now = Date.now();
    const licenseId = formatLicenseId();
    const license = {
      licenseId,
      pharmacyUuid: licenseId,
      mobileNumber,
      deviceId,
      firebaseUid: decoded.uid,
      pharmacyName: pharmacyName || '',
      ownerName: ownerName || '',
      plan: 'FREE',
      status: 'ACTIVE',
      activationDate: new Date(now).toISOString().slice(0, 10),
      expiryDate: new Date(now + ONE_YEAR_MS).toISOString().slice(0, 10),
      activationEpochMs: now,
      expiryEpochMs: now + ONE_YEAR_MS,
      branchUuid: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection('licenses').doc(license.licenseId).set(license);
    await syncAuthClaims(decoded.uid, license);
    return res.json(toApiResponse(license));
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: e.message });
  }
});

exports.verifyLicense = onRequest(async (req, res) => {
  try {
    if (req.method !== 'POST') return res.status(405).send('Method not allowed');
    const { idToken, licenseId, deviceId } = req.body;
    let decoded = null;
    if (idToken) decoded = await verifyFirebaseToken(idToken);
    const db = getDb();

    const doc = await db.collection('licenses').doc(licenseId).get();
    if (!doc.exists) return res.status(404).json({ error: 'NOT_FOUND' });

    const license = doc.data();
    if (license.deviceId !== deviceId) {
      return res.status(403).json({ error: 'DEVICE_MISMATCH' });
    }
    if (Date.now() > license.expiryEpochMs) {
      license.status = 'EXPIRED';
      await doc.ref.update({ status: 'EXPIRED' });
    } else if (decoded) {
      await syncAuthClaims(decoded.uid, license);
    }
    return res.json(toApiResponse(license));
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});

exports.transferLicense = onRequest(async (req, res) => {
  try {
    if (req.method !== 'POST') return res.status(405).send('Method not allowed');
    const { idToken, licenseId, newDeviceId, confirmTransfer } = req.body;
    const decoded = await verifyFirebaseToken(idToken);
    const db = getDb();

    if (!confirmTransfer) {
      return res.status(400).json({ error: 'CONFIRMATION_REQUIRED' });
    }

    const ref = db.collection('licenses').doc(licenseId);
    const doc = await ref.get();
    if (!doc.exists) return res.status(404).json({ error: 'NOT_FOUND' });

    await ref.update({
      deviceId: newDeviceId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    const updated = (await ref.get()).data();
    await syncAuthClaims(decoded.uid, updated);
    return res.json(toApiResponse(updated));
  } catch (e) {
    return res.status(500).json({ error: e.message });
  }
});

function toApiResponse(license) {
  return {
    licenseId: license.licenseId,
    mobileNumber: license.mobileNumber,
    deviceId: license.deviceId,
    pharmacyName: license.pharmacyName || '',
    ownerName: license.ownerName || '',
    plan: license.plan || 'FREE',
    status: license.status || 'ACTIVE',
    activationDate: license.activationDate,
    expiryDate: license.expiryDate,
    activationEpochMs: license.activationEpochMs,
    expiryEpochMs: license.expiryEpochMs,
  };
}
