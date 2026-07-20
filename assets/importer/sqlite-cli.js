'use strict';

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const SQLITE = process.env.SQLITE3_PATH || 'G:\\Android\\Sdk\\platform-tools\\sqlite3.exe';

function sqlFile(dbPath, query) {
  const q = query.replace(/"/g, '""');
  return execSync(`"${SQLITE}" "${dbPath}" "${q}"`, {
    encoding: 'utf8',
    maxBuffer: 512 * 1024 * 1024,
  }).trim();
}

function sqlRows(dbPath, query) {
  const out = execSync(`"${SQLITE}" -header -json "${dbPath}" "${query.replace(/"/g, '""')}"`, {
    encoding: 'utf8',
    maxBuffer: 512 * 1024 * 1024,
  });
  if (!out.trim()) return [];
  return JSON.parse(out);
}

function escapeSql(value) {
  if (value === null || value === undefined) return 'NULL';
  if (typeof value === 'number') return Number.isFinite(value) ? String(value) : 'NULL';
  return `'${String(value).replace(/'/g, "''")}'`;
}

function createDb(dbPath, ddl) {
  if (fs.existsSync(dbPath)) fs.unlinkSync(dbPath);
  const dir = path.dirname(dbPath);
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
  const tmp = dbPath + '.schema.sql';
  fs.writeFileSync(tmp, ddl);
  execSync(`"${SQLITE}" "${dbPath}" < "${tmp}"`, { stdio: 'pipe', shell: true });
  fs.unlinkSync(tmp);
}

function execBatch(dbPath, statements) {
  const tmp = dbPath + '.batch.sql';
  fs.writeFileSync(tmp, statements.join('\n'));
  execSync(`"${SQLITE}" "${dbPath}" < "${tmp}"`, { stdio: 'pipe', shell: true, maxBuffer: 512 * 1024 * 1024 });
  fs.unlinkSync(tmp);
}

function insertBatch(dbPath, table, columns, rows, batchSize = 500) {
  for (let i = 0; i < rows.length; i += batchSize) {
    const chunk = rows.slice(i, i + batchSize);
    const cols = columns.join(', ');
    const values = chunk
      .map((row) => `(${columns.map((c) => escapeSql(row[c])).join(', ')})`)
      .join(',\n');
    execBatch(dbPath, [`INSERT INTO ${table} (${cols}) VALUES ${values};`]);
  }
}

module.exports = { SQLITE, sqlFile, sqlRows, escapeSql, createDb, execBatch, insertBatch };
