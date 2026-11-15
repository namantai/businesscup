import { expect, test } from 'vitest';
import { exampleDetail } from './test-data';
import { basePrice, cardPrices } from './pricing';

function data() {
  const d = structuredClone(exampleDetail);
  const t = {
    ...d.offers[0].terms,
    amount: '20',
    upfrontAmount: '20',
    priceKind: 'FIXED' as const,
    currency: 'USD',
    billingPeriod: 'MONTH' as const,
    regionId: 'us',
    validFrom: '2026-01-01T00:00:00Z',
    validUntil: null,
  };
  d.offers = [
    {
      terms: t,
      monthlyEquivalent: '20',
      upfront: '20',
      allInMonthly: null,
      allInUpfront: null,
    },
  ];
  d.evidence = [
    { ...d.evidence[0], id: t.priceEvidenceId, effectiveStatus: 'CONDITIONAL' },
  ];
  return d;
}
const now = Date.parse('2026-11-15T00:00:00Z');
test('known base survives unknown checkout with evidence and its own currency', () => {
  const d = data();
  expect(cardPrices(d, {}, now).offers[0].terms.amount).toBe('20');
  expect(basePrice('20', 'USD', 'MONTH')).toBe('20 USD / мес.');
  expect(d.offers[0].allInMonthly).toBeNull();
  expect(cardPrices(d, { regionId: 'kz' }, now).offers).toEqual([]);
  expect(cardPrices(d, { currency: 'KZT' }, now).offers).toEqual([]);
});
test('unknown, stale, conflicting, expired and trial prices cannot advertise a current free tier', () => {
  for (const status of ['UNKNOWN', 'STALE', 'CONFLICTED', 'DEMO']) {
    const d = data();
    d.evidence[0].effectiveStatus = status;
    expect(cardPrices(d, {}, now).offers).toEqual([]);
  }
  for (const kind of ['UNKNOWN', 'TRIAL'] as const) {
    const d = data();
    d.offers[0].terms.priceKind = kind;
    expect(cardPrices(d, {}, now).offers).toEqual([]);
  }
  const d = data();
  d.offers[0].terms.validUntil = '2026-01-02T00:00:00Z';
  expect(cardPrices(d, {}, now).offers).toEqual([]);
  expect(basePrice(null, 'USD', 'MONTH')).toBe('Цена не подтверждена');
});
test('annual charge is not advertised as a monthly payment and decimal ordering stays exact', () => {
  const d = data();
  d.offers[0].terms.billingPeriod = 'YEAR';
  d.offers[0].terms.amount = '96';
  expect(
    basePrice(cardPrices(d, {}, now).offers[0].terms.amount, 'USD', 'YEAR'),
  ).toBe('96 USD / год');
  d.offers[0].terms.amount = '20.000002';
  d.offers.push({
    ...d.offers[0],
    terms: { ...d.offers[0].terms, id: 'less', amount: '20.000001' },
  });
  expect(cardPrices(d, {}, now).offers[0].terms.id).toBe('less');
  d.offers.push({
    ...d.offers[0],
    terms: { ...d.offers[0].terms, id: 'cny', currency: 'CNY', amount: '1' },
  });
  expect(cardPrices(d, {}, now).offers).toHaveLength(2);
});
