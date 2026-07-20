'use strict';

const XLSX = require('xlsx');
const { parseProductName, norm } = require('./utils');

/**
 * Reads pharmacy price list from product_list.xlsx.
 */
class ExcelReader {
  constructor(xlsxPath) {
    this.xlsxPath = xlsxPath;
    this.records = [];
  }

  read() {
    const wb = XLSX.readFile(this.xlsxPath);
    const sheetName = wb.SheetNames[0];
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[sheetName], { defval: '' });

    this.records = rows
      .map((row, idx) => {
        const productName = String(row.Product_name ?? row.product_name ?? '').trim();
        const salesPrice = parseFloat(String(row.Sales_price ?? row.sales_price ?? '0').replace(/,/g, '')) || 0;
        const parsed = parseProductName(productName);
        return {
          rowNum: idx + 2,
          productName,
          salesPrice,
          parsedBrand: parsed.brandName,
          parsedStrength: parsed.strength,
          parsedForm: parsed.dosageForm,
          normName: norm(productName),
          normBrand: norm(parsed.brandName),
          brandToken: norm(parsed.brandName).split(' ')[0],
        };
      })
      .filter((r) => r.productName);

    return {
      sheetName,
      total: this.records.length,
      withPrice: this.records.filter((r) => r.salesPrice > 0).length,
    };
  }
}

module.exports = { ExcelReader };
