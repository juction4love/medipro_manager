'use strict';

const fs = require('fs');
const { createDb, insertBatch, sqlFile } = require('./sqlite-cli');
const { MEDICINES_DDL } = require('./schema');

class RoomSeedWriter {
  constructor(outputPath) {
    this.outputPath = outputPath;
  }

  write(entities) {
    createDb(this.outputPath, MEDICINES_DDL);

    const columns = [
      'uuid', 'brandName', 'genericName', 'composition', 'strength', 'dosageForm', 'manufacturer',
      'category', 'barcode', 'unit', 'purchasePricePaisa', 'sellingPricePaisa', 'mrpPaisa',
      'vatPercent', 'reorderLevel', 'description', 'requiresPrescription', 'controlledSubstance',
      'scheduleCategory', 'isActive', 'createdAt', 'updatedAt', 'deletedAt', 'syncStatus', 'syncVersion', 'deviceId',
    ];

    const seenBarcodes = new Set();
    const rows = entities.map(({ _meta, ...e }) => {
      const row = { ...e };
      if (row.barcode) {
        const key = String(row.barcode).trim();
        if (!key || seenBarcodes.has(key)) {
          row.barcode = null;
        } else {
          seenBarcodes.add(key);
        }
      }
      return row;
    });

    insertBatch(this.outputPath, 'medicines', columns, rows, 200);

    const count = parseInt(sqlFile(this.outputPath, 'SELECT COUNT(*) FROM medicines;'), 10);
    return {
      path: this.outputPath,
      count,
      sizeMb: +(fs.statSync(this.outputPath).size / 1024 / 1024).toFixed(2),
    };
  }
}

module.exports = { RoomSeedWriter };
