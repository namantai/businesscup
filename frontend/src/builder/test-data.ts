import fixture from '../../../data/fixtures/recommendation-demo.json';
import type { BuilderCatalog } from '../api/builder';
import type { Bundle, RecommendationResult } from '../api/generated/types.gen';
// Synthetic fixtures exclusively for tests; runtime components do not import this file.
export const catalog = {
  version: fixture.versionId,
  products: fixture.products,
  plans: fixture.subscriptionPlans,
  offers: fixture.priceOffers,
  models: fixture.aiModels,
  agents: fixture.agents,
  capabilities: fixture.capabilities,
  needs: fixture.userNeeds,
  limits: fixture.usageLimits,
} as BuilderCatalog;
export const meta = {
  versionId: fixture.versionId,
  revision: 1,
  datasetKind: 'DEMO',
  status: 'AVAILABLE',
  asOf: fixture.asOf,
  regions: fixture.regions,
  needs: fixture.userNeeds,
  capabilities: fixture.capabilities,
  productCount: fixture.products.length,
  unknownClaims: 1,
  staleClaims: 0,
};
export function bundle(id = 'one'): Bundle {
  return {
    id,
    strategies: ['ECONOMY', 'BALANCED', 'EXPANDED'],
    eligibility: 'ELIGIBLE',
    items: [
      {
        id: 'learner-offer',
        productId: 'learner',
        planId: 'learner-plan',
        offerId: 'learner-offer',
        action: 'ADD',
        originalCurrency: 'USD',
        originalCharge: '0',
        billingPeriod: 'MONTH',
        nextRenewal: null,
        includedAgentIds: fixture.agents.map((a) => a.id),
        modelIds: fixture.aiModels.map((m) => m.id),
        sharedPoolIds: [],
        evidenceIds: fixture.evidence.map((e) => e.id),
      },
    ],
    costs: {
      currency: 'USD',
      cashDueNow: '0',
      upfront: '0',
      recurringMonthly: '0',
      horizonTotal: '0',
      futureMonthly: '0',
      comparableBaselineMonthly: null,
      potentialNetMonthlySavings: null,
      compassFeeMonthly: '0',
      confidence: 'CONFIRMED_COMPONENTS',
      confirmedBudgetFit: true,
    },
    coverage: [
      {
        capabilityId: 'coding',
        weight: 3,
        required: true,
        state: 'SUPPORTED',
        confirmed: true,
        acceptedForRequirement: true,
        evidenceIds: [],
      },
    ],
    usageFit: 'UNKNOWN',
    factualReasons: [
      {
        code: 'WITHIN_BUDGET',
        detail: 'Synthetic confirmed costs',
        evidenceIds: [],
      },
    ],
    policyReasons: [],
    conditions: [],
    portfolioActions: [],
  };
}
export function result(count = 1): RecommendationResult {
  return {
    requestId: 'synthetic-ui-test',
    catalogVersion: catalog.version,
    datasetKind: 'DEMO',
    engineVersion: 'test',
    policyVersion: 'test',
    asOf: fixture.asOf,
    searchComplete: true,
    search: {
      catalogProducts: 8,
      candidateCount: 8,
      consideredCandidates: 8,
      visitedCombinations: 9,
      feasibleCombinations: count,
      dominatedCombinations: 0,
      limitation: '',
    },
    status: count ? 'MATCH' : 'NO_MATCH',
    bundles: Array.from({ length: count }, (_, i) => bundle(String(i))),
    conditionalAlternatives: [],
    unmetRequirements: [],
    warnings: [],
    assessments: [],
    evidence: fixture.evidence.map((e) => ({
      ...e,
      effectiveStatus: e.status,
    })),
    sources: fixture.evidenceSources,
    fxSnapshots: [],
  };
}
export function catalogResponse(url: URL) {
  if (url.pathname.endsWith('/meta')) return meta;
  const key = url.pathname.split('/').at(-1);
  const rows: Record<string, unknown[]> = {
    products: catalog.products,
    plans: catalog.plans,
    offers: catalog.offers.map((terms) => ({
      terms,
      monthlyEquivalent: null,
      allInMonthly: null,
      allInUpfront: null,
      upfront: null,
    })),
    models: catalog.models,
    agents: catalog.agents,
    capabilities: catalog.capabilities,
    needs: catalog.needs,
    'usage-limits': catalog.limits,
  };
  const items = rows[key ?? ''];
  if (!items) throw new Error(`Unexpected test endpoint: ${url.pathname}`);
  return {
    items,
    versionId: catalog.version,
    total: items.length,
    page: 0,
    size: 100,
  };
}
