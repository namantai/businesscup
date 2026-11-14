import { createRequire } from 'node:module';
import { readFileSync, readdirSync } from 'node:fs';

// Reuse the exact Ajv 6.15.0 already locked by ESLint; never fetch remote schemas.
const require = createRequire(new URL('../frontend/package.json', import.meta.url));
const Ajv = require('ajv');
const schema = JSON.parse(readFileSync(new URL('../data/schema/catalog.schema.json', import.meta.url)));
const fixture = JSON.parse(readFileSync(new URL('../data/fixtures/catalog-demo.json', import.meta.url)));
const validate = new Ajv({ allErrors: true, schemaId: 'auto' }).compile(schema);
if (!validate(fixture)) throw new Error(JSON.stringify(validate.errors));
const invalidCases = [
  d => { delete d.versionId; },
  d => { d.schemaVersion = 2; },
  d => { d.unknownField = true; },
  d => { d.priceOffers[0].amount = 12.34; },
  d => { d.priceOffers[0].amount = '-1'; },
  d => { d.priceOffers[0].amount = '0.0000001'; },
  d => { d.priceOffers[0].currency = 'usd'; },
  d => { d.priceOffers[0].priceKind = 'GUESS'; },
  d => { d.evidence[0].verifiedAt = 'yesterday'; },
  d => { d.priceOffers[0].amount = null; },
  d => { d.priceOffers.find(o => o.priceKind === 'UNKNOWN').amount = '0'; },
  d => { d.priceOffers.find(o => o.priceKind === 'TRIAL').trialDays = null; },
];
for (const mutate of invalidCases) {
  const invalid = structuredClone(fixture);
  mutate(invalid);
  if (validate(invalid)) throw new Error('Invalid schema example was accepted');
}
console.log(`PASS: catalog JSON Schema, 1 synthetic manifest + ${invalidCases.length} negative cases.`);

for (const file of readdirSync(new URL('../data/catalog/', import.meta.url)).filter(f => f.endsWith('.json'))) {
  const manifest = JSON.parse(readFileSync(new URL(`../data/catalog/${file}`, import.meta.url)));
  if (!validate(manifest)) throw new Error(`${file}: ${JSON.stringify(validate.errors)}`);
  if (manifest.datasetKind !== 'REVIEWED') throw new Error(`${file}: reviewed catalog required`);
  console.log(`PASS: reviewed manifest schema ${file}`);
}

const recommendation = JSON.parse(readFileSync(new URL('../data/fixtures/recommendation-demo.json', import.meta.url)));
if (!validate(recommendation) || recommendation.datasetKind !== 'DEMO') throw new Error('Invalid synthetic recommendation catalog: ' + JSON.stringify(validate.errors));
console.log('PASS: synthetic recommendation catalog schema (never production seed).');
