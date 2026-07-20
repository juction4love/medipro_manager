'use strict';

const UNKNOWN = 'Unknown';

function norm(s) {
  return String(s ?? '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .replace(/[^\w\s+./%-]/g, '');
}

function isUnknown(value) {
  const v = String(value ?? '').trim();
  return !v || norm(v) === 'unknown';
}

function firstToken(s) {
  const n = norm(s);
  return n.split(' ')[0] || n;
}

function uuid() {
  return crypto.randomUUID();
}

const DOSAGE_FORMS = [
  'TABLET', 'TAB', 'CAPSULE', 'CAP', 'INJECTION', 'INJ', 'SYRUP', 'SYP', 'SUSPENSION', 'SUSP',
  'CREAM', 'GEL', 'OINTMENT', 'DROPS', 'SPRAY', 'POWDER', 'PWD', 'LOTION', 'SOLUTION', 'SOL',
  'INHALER', 'SUPPOSITORY', 'SUP', 'PATCH', 'SACHET', 'VIAL', 'AMP', 'AMPOULE', 'SET',
];

const FORM_MAP = {
  TAB: 'Tablet',
  CAP: 'Capsule',
  INJ: 'Injection',
  SYP: 'Syrup',
  SUSP: 'Suspension',
  PWD: 'Powder',
  SET: 'Set',
};

function parseProductName(raw) {
  let name = String(raw ?? '').trim().replace(/\s+/g, ' ');
  const upper = name.toUpperCase();
  let dosageForm = '';

  for (const form of [...DOSAGE_FORMS].sort((a, b) => b.length - a.length)) {
    const re = new RegExp(`\\b${form}\\b`, 'i');
    if (re.test(upper)) {
      dosageForm = FORM_MAP[form.toUpperCase()] || form;
      name = name.replace(re, ' ').replace(/\s+/g, ' ').trim();
      break;
    }
  }

  let strength = '';
  const sm =
    name.match(/\b(\d+(?:\.\d+)?(?:MG|GM|G|ML|MCG|IU|%|W\/W)(?:\/\d+(?:\.\d+)?(?:MG|ML))?)\b/i) ||
    name.match(/\b(\d+(?:\.\d+)?)\s*(ML|MG|GM|G)\b/i);

  if (sm) {
    strength = sm[0].replace(/\s+/g, '').toUpperCase();
    name = name.replace(sm[0], ' ').replace(/\s+/g, ' ').trim();
  } else {
    const numOnly = name.match(/\b(\d+(?:\.\d+)?)\s*$/);
    if (numOnly) {
      strength = numOnly[1];
      name = name.replace(numOnly[0], '').trim();
    }
  }

  return {
    brandName: name.trim(),
    strength,
    dosageForm,
    raw: String(raw ?? '').trim(),
  };
}

function medicineKey(brand, generic, strength, form) {
  return norm(`${brand}|${generic}|${strength}|${form}`);
}

module.exports = {
  UNKNOWN,
  norm,
  isUnknown,
  firstToken,
  uuid,
  parseProductName,
  medicineKey,
};
