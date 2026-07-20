const XLSX = require('xlsx');
const wb = XLSX.readFile('G:\\medipro\\assets\\product_list.xlsx');
const sheet = wb.Sheets[wb.SheetNames[0]];
const rows = XLSX.utils.sheet_to_json(sheet, { defval: '' });
console.log('Sheet:', wb.SheetNames[0]);
console.log('Rows:', rows.length);
console.log('Headers:', Object.keys(rows[0] || {}));
console.log('First 15 rows:');
rows.slice(0, 15).forEach((r, i) => console.log(i + 1, JSON.stringify(r)));
