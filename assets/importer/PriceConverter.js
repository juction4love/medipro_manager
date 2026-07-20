'use strict';

/**
 * Converts NPR decimal price to paisa (Long).
 * Example: 298.72 NPR → 29872 paisa (×100, no currency conversion).
 */
class PriceConverter {
  static toPaisa(nprAmount) {
    const n = parseFloat(String(nprAmount ?? '').replace(/,/g, ''));
    if (Number.isNaN(n) || n <= 0) return 0;
    return Math.round(n * 100);
  }

  static fromPaisa(paisa) {
    return paisa / 100;
  }
}

module.exports = { PriceConverter };
