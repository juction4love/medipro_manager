const fs = require('fs');
const path = require('path');

const schemaDir = path.join(__dirname, '../core/database/schemas/com.medipro.manager.core.database.MediProDatabase');

function load(v) {
  return JSON.parse(fs.readFileSync(path.join(schemaDir, `${v}.json`), 'utf8'));
}

function columns(entity) {
  return Object.fromEntries(entity.fields.map(f => [f.columnName, f]));
}

for (const step of [[4,5],[3,4],[2,3],[9,10]]) {
  const [from, to] = step;
  const old = load(from);
  const neu = load(to);
  console.log(`\n=== ${from} -> ${to} ===`);
  for (const entity of neu.database.entities) {
    const prev = old.database.entities.find(e => e.tableName === entity.tableName);
    if (!prev) {
      console.log(`NEW TABLE ${entity.tableName}`);
      continue;
    }
    const oldCols = columns(prev);
    const newCols = columns(entity);
    const added = Object.keys(newCols).filter(c => !(c in oldCols));
    if (added.length) {
      console.log(`${entity.tableName}: +${added.join(', ')}`);
    }
    const oldIdx = new Set((prev.indices || []).map(i => i.name));
    const newIdx = (entity.indices || []).filter(i => !oldIdx.has(i.name));
    for (const idx of newIdx) {
      console.log(`INDEX ${entity.tableName}: ${idx.createSql.replace(/\`\$\{TABLE_NAME\}\`/g, entity.tableName)}`);
    }
  }
}

console.log('\n=== 8 -> 9 indexes ===');
const v8 = load(8);
const v9 = load(9);
for (const entity of v9.database.entities) {
  const prev = v8.database.entities.find(e => e.tableName === entity.tableName);
  if (!prev) continue;
  const oldIdx = new Set((prev.indices || []).map(i => i.name));
  for (const idx of entity.indices || []) {
    if (!oldIdx.has(idx.name)) {
      console.log(idx.createSql.replace(/\`\$\{TABLE_NAME\}\`/g, entity.tableName));
    }
  }
}
