import assert from 'node:assert/strict';
import { test } from 'vitest';
import { quickMatches, pickBundle, pricePresentation } from './matching';
const now = new Date('2026-11-15T00:00:00Z');
const plans = [
  {
    id: 'gemini-free',
    productId: 'gemini',
    tier: 'Free',
    accessType: 'APPLICATION',
    activeFrom: '2026-01-01T00:00:00Z',
    activeUntil: null,
  },
  {
    id: 'canva-free',
    productId: 'canva',
    tier: 'Free',
    accessType: 'APPLICATION',
    activeFrom: '2026-01-01T00:00:00Z',
    activeUntil: null,
  },
];
const goals = ['study', 'development', 'presentations', 'research'];
const catalog = {
  version: 'test',
  products: [
    {
      id: 'gemini',
      slug: 'gemini',
      name: 'Gemini',
      state: 'ACTIVE',
      kind: 'APPLICATION',
      providerId: 'google',
    },
    {
      id: 'canva',
      slug: 'canva',
      name: 'Canva',
      state: 'ACTIVE',
      kind: 'APPLICATION',
      providerId: 'canva',
    },
  ],
  plans,
  offers: plans.map((p) => ({
    terms: {
      id: p.id + '-offer',
      planId: p.id,
      regionId: 'us',
      currency: 'USD',
      channel: 'WEB',
      priceKind: 'FREE',
      billingPeriod: 'MONTH',
      amount: '0',
      upfrontAmount: '0',
      priceEvidenceId: 'price',
      validFrom: '2026-01-01T00:00:00Z',
      validUntil: null,
    },
  })),
  needs: goals.map((id) => ({ id, name: id })),
  mappings: goals.map((id) => ({ needId: id, capabilityId: id })),
  claims: [
    ...['study', 'development', 'research'].map((capabilityId) => ({
      id: capabilityId,
      planId: 'gemini-free',
      productId: null,
      capabilityId,
      state: 'SUPPORTED',
      evidenceId: 'verified',
    })),
    {
      id: 'presentations',
      planId: 'canva-free',
      productId: null,
      capabilityId: 'presentations',
      state: 'SUPPORTED',
      evidenceId: 'verified',
    },
  ],
  evidence: [
    {
      id: 'verified',
      sourceId: 's',
      verifiedAt: '2026-11-14T00:00:00Z',
      effectiveStatus: 'VERIFIED_SOURCE',
    },
    {
      id: 'price',
      sourceId: 's',
      verifiedAt: '2026-11-14T00:00:00Z',
      effectiveStatus: 'CONDITIONAL',
    },
  ],
  sources: [{ id: 's', publisher: 'Official', url: 'https://example.org/' }],
};
test('filter and rank by source-supported functional coverage, not by name', () => {
  const matches = quickMatches(catalog, goals, '20', 'USD', 'KZ', now);
  assert.equal(matches.length, 2);
  assert.deepEqual(matches[0].matches, ['study', 'development', 'research']);
  assert.deepEqual(
    pickBundle(matches, goals).items.map((m) => m.product.name),
    ['Gemini', 'Canva'],
  );
  assert.deepEqual(pickBundle(matches, goals).missing, []);
});
test('known foreign free list price is not claimed as local checkout', () => {
  const matches = quickMatches(catalog, goals, '20', 'USD', 'KZ', now);
  const x = pricePresentation(matches[0], 'KZ');
  assert.equal(x.headline, 'Есть бесплатный план');
  assert.match(x.detail, /не цена KZ/);
});
test('unknown evidence never confirms a capability', () => {
  const next = {
    ...catalog,
    evidence: catalog.evidence.map((e) =>
      e.id === 'verified' ? { ...e, effectiveStatus: 'UNKNOWN' } : e,
    ),
  };
  assert.deepEqual(
    quickMatches(next, goals, '20', 'USD', 'KZ', now)[0].matches,
    [],
  );
});
test('UI selection keeps all products visible even when none qualifies', () => {
  const matches = quickMatches(catalog, ['video'], '50', 'USD', 'KZ', now);
  assert.equal(matches.length, 2);
  assert.equal(
    matches.every((m) => m.matches.length === 0),
    true,
  );
  assert.deepEqual(pickBundle(matches, ['video']).missing, ['video']);
});
test('foreign list price above budget is flagged, not silently promoted to checkout', () => {
  const x = {
    ...catalog,
    offers: catalog.offers.map((o) =>
      o.terms.planId === 'gemini-free'
        ? { terms: { ...o.terms, priceKind: 'FIXED', amount: '30' } }
        : o,
    ),
  };
  const matches = quickMatches(
    x,
    ['study', 'development'],
    '20',
    'USD',
    'KZ',
    now,
  );
  assert.equal(
    matches.find((m) => m.product.id === 'gemini')!.budgetFit,
    'above-budget',
  );
  assert.match(
    pricePresentation(
      matches.find((m) => m.product.id === 'gemini')!,
      'KZ',
    ).detail,
    /не цена KZ/,
  );
});

test('decimal budget is exact and invalid budget does not remove products', () => {
  const x = {
    ...catalog,
    offers: catalog.offers.map((o) => ({
      terms: { ...o.terms, priceKind: 'FIXED', amount: '20.000001' },
    })),
  };
  assert.equal(
    quickMatches(x, goals, '20', 'USD', 'US', now)[0].budgetFit,
    'above-budget',
  );
  assert.equal(
    quickMatches(x, goals, '20.000001', 'USD', 'US', now)[0].budgetFit,
    'indicative',
  );
  const invalid = quickMatches(x, goals, null, 'USD', 'US', now);
  assert.equal(invalid.length, 2);
  assert.ok(invalid.every((m) => m.budgetFit === 'not-comparable'));
});
test('different currencies and annual upfront never become comparable monthly prices', () => {
  const x = {
    ...catalog,
    offers: catalog.offers.map((o) => ({
      terms: {
        ...o.terms,
        priceKind: 'FIXED',
        amount: '96',
        billingPeriod: 'YEAR',
      },
    })),
  };
  const m = quickMatches(x, goals, '20', 'KZT', 'KZ', now)[0];
  assert.equal(m.budgetFit, 'not-comparable');
  assert.match(pricePresentation(m, 'KZ').headline, /96.*год/);
  assert.match(pricePresentation(m, 'KZ').detail, /оплата за год/);
  const multi = {
    ...m,
    prices: [
      { ...m.prices[0], currency: 'USD', amount: '20', period: 'MONTH' },
      { ...m.prices[0], currency: 'CNY', amount: '10', period: 'MONTH' },
    ],
  };
  assert.equal(pricePresentation(multi, 'KZ', 'USD').price!.currency, 'USD');
  assert.equal(pricePresentation(multi, 'KZ', 'CNY').price!.currency, 'CNY');
});
test('expired or unknown price stays unknown; unpriced products stay visible', () => {
  const x = {
    ...catalog,
    offers: catalog.offers.map((o) => ({
      terms: { ...o.terms, validUntil: '2026-01-02T00:00:00Z' },
    })),
    products: [
      ...catalog.products,
      { ...catalog.products[0], id: 'unpriced', slug: 'unpriced' },
    ],
  };
  const m = quickMatches(x, goals, '20', 'USD', 'US', now);
  assert.equal(m.length, 3);
  assert.ok(m.every((x) => x.prices.length === 0));
  assert.equal(m.find((x) => x.product.id === 'unpriced')!.matches.length, 0);
});
test('plan-specific unknown overrides product support and conflicting evidence cannot confirm', () => {
  const x = {
    ...catalog,
    claims: [
      ...catalog.claims.map((c) =>
        c.id === 'study' ? { ...c, state: 'UNKNOWN' } : c,
      ),
      {
        id: 'product-study',
        productId: 'gemini',
        planId: null,
        capabilityId: 'study',
        state: 'SUPPORTED',
        evidenceId: 'verified',
      },
    ],
  };
  assert.equal(
    quickMatches(x, ['study'], '20', 'USD', 'US', now).some(
      (m) => m.matches.length,
    ),
    false,
  );
  const conflict = {
    ...catalog,
    claims: [
      ...catalog.claims,
      { ...catalog.claims[0], id: 'conflict', state: 'UNSUPPORTED' },
    ],
  };
  assert.equal(
    quickMatches(conflict, ['study'], '20', 'USD', 'US', now).some(
      (m) => m.matches.length,
    ),
    false,
  );
});
