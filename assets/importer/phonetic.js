'use strict';

/** Lightweight phonetic key — mirrors Kotlin [PhoneticEncoder] for catalog ETL. */
function encodePhonetic(text) {
  if (!text || !String(text).trim()) return '';

  const multi = [
    ['ph', 'f'], ['gh', 'g'], ['kh', 'k'], ['dh', 'd'], ['bh', 'b'],
    ['th', 't'], ['sh', 's'], ['ch', 'c'], ['ck', 'k'], ['sch', 's'],
    ['tion', 'n'], ['sion', 'n'],
  ];
  const similar = { c: 'k', q: 'k', x: 'k', z: 's', v: 'f', w: 'v', j: 'g', y: 'i' };
  const vowels = new Set(['a', 'e', 'i', 'o', 'u']);

  let normalized = String(text).toLowerCase().replace(/[^a-z0-9\s]/g, ' ').trim();
  multi.forEach(([from, to]) => {
    normalized = normalized.split(from).join(to);
  });

  const tokens = normalized.split(/\s+/).filter(Boolean);
  return tokens.map((token) => encodeToken(token, vowels, similar)).join(' ').trim();
}

function encodeToken(token, vowels, similar) {
  if (!token) return '';
  const norm = (ch) => similar[ch] || ch;
  let previous = norm(token[0]);
  let out = token[0];
  for (let i = 1; i < token.length; i += 1) {
    const ch = norm(token[i]);
    if (vowels.has(ch)) continue;
    if (ch === previous) continue;
    out += ch;
    previous = ch;
  }
  return out;
}

module.exports = { encodePhonetic };
