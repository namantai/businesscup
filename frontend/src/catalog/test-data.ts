import fixture from '../../../data/fixtures/catalog-demo.json';
import type { Detail, Meta, PageProduct } from '../api/generated/types.gen';

// Synthetic API responses only. Production components never import this module.
export const exampleDetail = {
  versionId: fixture.versionId,
  product: fixture.products[0],
  plans: fixture.subscriptionPlans,
  offers: fixture.priceOffers.map((terms) => ({
    terms,
    monthlyEquivalent:
      terms.billingPeriod === 'YEAR' ? '10.000834' : terms.amount,
    upfront: terms.upfrontAmount,
    allInMonthly: null,
    allInUpfront: null,
  })),
  assertions: fixture.capabilityAssertions,
  capabilities: fixture.capabilities,
  modelAccess: fixture.planModelAccess,
  models: fixture.aiModels,
  agentAccess: fixture.planAgentAccess,
  agents: fixture.agents,
  quotaPools: fixture.quotaPools,
  usageLimits: fixture.usageLimits,
  eligibilityRules: fixture.eligibilityRules,
  regions: fixture.regions,
  evidence: fixture.evidence.map((e) => ({ ...e, effectiveStatus: e.status })),
  sources: fixture.evidenceSources,
} as Detail;
export const exampleMeta: Meta = {
  versionId: fixture.versionId,
  revision: 1,
  datasetKind: 'DEMO',
  asOf: fixture.asOf,
  status: 'AVAILABLE',
  regions: fixture.regions,
  needs: fixture.userNeeds,
  capabilities: exampleDetail.capabilities,
  productCount: 2,
  unknownClaims: 4,
  staleClaims: 1,
};
export const examplePage = {
  versionId: fixture.versionId,
  items: fixture.products,
  total: 2,
  page: 0,
  size: 12,
} as PageProduct;
