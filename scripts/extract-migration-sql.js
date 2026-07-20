const fs = require('fs');
const path = require('path');

const schemaDir = path.join(__dirname, '../core/database/schemas/com.medipro.manager.core.database.MediProDatabase');
const tables = ['purchase_returns', 'purchase_return_items', 'sale_returns', 'sale_return_items', 'stock_adjustments'];

for (const v of [6, 7, 8]) {
  const json = JSON.parse(fs.readFileSync(path.join(schemaDir, `${v}.json`), 'utf8'));
  for (const entity of json.database.entities) {
    if (!tables.includes(entity.tableName)) continue;
    console.log(`--- v${v} ${entity.tableName}`);
    console.log(entity.createSql.replace(/\`\$\{TABLE_NAME\}\`/g, entity.tableName));
    for (const idx of entity.indices || []) {
      console.log(idx.createSql.replace(/\`\$\{TABLE_NAME\}\`/g, entity.tableName));
    }
  }
}
