'use strict';

/** Room v2 medicines table + FTS4 (matches MediProDatabase schema export). */
const MEDICINES_DDL = `
CREATE TABLE IF NOT EXISTS medicines (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  uuid TEXT NOT NULL,
  brandName TEXT NOT NULL,
  genericName TEXT NOT NULL,
  composition TEXT NOT NULL,
  strength TEXT NOT NULL,
  dosageForm TEXT NOT NULL,
  manufacturer TEXT NOT NULL,
  category TEXT NOT NULL,
  barcode TEXT,
  unit TEXT NOT NULL,
  purchasePricePaisa INTEGER NOT NULL,
  sellingPricePaisa INTEGER NOT NULL,
  mrpPaisa INTEGER NOT NULL,
  vatPercent REAL NOT NULL,
  reorderLevel INTEGER NOT NULL,
  description TEXT,
  requiresPrescription INTEGER NOT NULL,
  controlledSubstance INTEGER NOT NULL,
  scheduleCategory TEXT NOT NULL,
  isActive INTEGER NOT NULL,
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL,
  deletedAt INTEGER,
  syncStatus TEXT NOT NULL,
  syncVersion INTEGER NOT NULL,
  deviceId TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS index_medicines_uuid ON medicines (uuid);
CREATE UNIQUE INDEX IF NOT EXISTS index_medicines_barcode ON medicines (barcode);
CREATE INDEX IF NOT EXISTS index_medicines_brandName ON medicines (brandName);
CREATE INDEX IF NOT EXISTS index_medicines_genericName ON medicines (genericName);
CREATE INDEX IF NOT EXISTS index_medicines_composition ON medicines (composition);
CREATE INDEX IF NOT EXISTS index_medicines_manufacturer ON medicines (manufacturer);
CREATE INDEX IF NOT EXISTS index_medicines_syncStatus ON medicines (syncStatus);

CREATE VIRTUAL TABLE IF NOT EXISTS medicines_fts USING fts4(
  brandName TEXT NOT NULL,
  genericName TEXT NOT NULL,
  composition TEXT NOT NULL,
  strength TEXT NOT NULL,
  manufacturer TEXT NOT NULL,
  barcode TEXT,
  content='medicines'
);

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_BEFORE_UPDATE BEFORE UPDATE ON medicines
BEGIN DELETE FROM medicines_fts WHERE docid=OLD.rowid; END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_BEFORE_DELETE BEFORE DELETE ON medicines
BEGIN DELETE FROM medicines_fts WHERE docid=OLD.rowid; END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_AFTER_UPDATE AFTER UPDATE ON medicines
BEGIN INSERT INTO medicines_fts(docid, brandName, genericName, composition, strength, manufacturer, barcode)
VALUES (NEW.rowid, NEW.brandName, NEW.genericName, NEW.composition, NEW.strength, NEW.manufacturer, NEW.barcode); END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medicines_fts_AFTER_INSERT AFTER INSERT ON medicines
BEGIN INSERT INTO medicines_fts(docid, brandName, genericName, composition, strength, manufacturer, barcode)
VALUES (NEW.rowid, NEW.brandName, NEW.genericName, NEW.composition, NEW.strength, NEW.manufacturer, NEW.barcode); END;
`;

/** Read-only master catalog (271K) for offline search. */
const CATALOG_DDL = `
CREATE TABLE IF NOT EXISTS catalog_medicines (
  id INTEGER PRIMARY KEY NOT NULL,
  medicineUuid TEXT NOT NULL,
  brandName TEXT NOT NULL,
  genericName TEXT NOT NULL,
  composition TEXT NOT NULL,
  compositionTokens TEXT NOT NULL DEFAULT '',
  strength TEXT NOT NULL,
  dosageForm TEXT NOT NULL,
  manufacturer TEXT NOT NULL,
  category TEXT NOT NULL,
  barcode TEXT,
  phoneticBrand TEXT NOT NULL DEFAULT '',
  phoneticGeneric TEXT NOT NULL DEFAULT ''
);

CREATE UNIQUE INDEX IF NOT EXISTS index_catalog_medicineUuid ON catalog_medicines (medicineUuid);
CREATE UNIQUE INDEX IF NOT EXISTS index_catalog_barcode_unique ON catalog_medicines (barcode) WHERE barcode IS NOT NULL AND barcode != '';
CREATE INDEX IF NOT EXISTS index_catalog_brandName ON catalog_medicines (brandName);
CREATE INDEX IF NOT EXISTS index_catalog_genericName ON catalog_medicines (genericName);
CREATE INDEX IF NOT EXISTS index_catalog_manufacturer ON catalog_medicines (manufacturer);
CREATE INDEX IF NOT EXISTS index_catalog_compositionTokens ON catalog_medicines (compositionTokens);
CREATE INDEX IF NOT EXISTS index_catalog_phoneticBrand ON catalog_medicines (phoneticBrand);
CREATE INDEX IF NOT EXISTS index_catalog_phoneticGeneric ON catalog_medicines (phoneticGeneric);

CREATE TABLE IF NOT EXISTS catalog_synonyms (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  term TEXT NOT NULL,
  canonical TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS index_catalog_synonyms_term ON catalog_synonyms (term);
CREATE INDEX IF NOT EXISTS index_catalog_synonyms_canonical ON catalog_synonyms (canonical);

CREATE VIRTUAL TABLE IF NOT EXISTS catalog_medicines_fts USING fts4(
  brandName TEXT NOT NULL,
  genericName TEXT NOT NULL,
  composition TEXT NOT NULL,
  strength TEXT NOT NULL,
  manufacturer TEXT NOT NULL,
  barcode TEXT,
  content='catalog_medicines'
);

CREATE TRIGGER IF NOT EXISTS catalog_fts_after_insert AFTER INSERT ON catalog_medicines
BEGIN INSERT INTO catalog_medicines_fts(docid, brandName, genericName, composition, strength, manufacturer, barcode)
VALUES (NEW.id, NEW.brandName, NEW.genericName, NEW.composition, NEW.strength, NEW.manufacturer, NEW.barcode); END;

CREATE TRIGGER IF NOT EXISTS catalog_fts_before_delete BEFORE DELETE ON catalog_medicines
BEGIN DELETE FROM catalog_medicines_fts WHERE docid=OLD.id; END;

CREATE TRIGGER IF NOT EXISTS catalog_fts_before_update BEFORE UPDATE ON catalog_medicines
BEGIN DELETE FROM catalog_medicines_fts WHERE docid=OLD.id; END;

CREATE TRIGGER IF NOT EXISTS catalog_fts_after_update AFTER UPDATE ON catalog_medicines
BEGIN INSERT INTO catalog_medicines_fts(docid, brandName, genericName, composition, strength, manufacturer, barcode)
VALUES (NEW.id, NEW.brandName, NEW.genericName, NEW.composition, NEW.strength, NEW.manufacturer, NEW.barcode); END;
`;

module.exports = { MEDICINES_DDL, CATALOG_DDL };
