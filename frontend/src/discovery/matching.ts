/* A fast, explanatory catalog shortlist. NOT a purchase eligibility or model quality rating. */
export type Product = {
  id: string;
  name: string;
  slug: string;
  state: string;
  kind: string;
  providerId: string;
};
export type Plan = {
  id: string;
  productId: string;
  tier: string;
  accessType: string;
  activeFrom: string;
  activeUntil: string | null;
};
export type Offer = {
  terms: {
    id: string;
    planId: string;
    regionId: string;
    currency: string;
    channel: string;
    priceKind: string;
    billingPeriod: string;
    amount: string | null;
    upfrontAmount: string | null;
    priceEvidenceId: string;
    validFrom: string;
    validUntil: string | null;
  };
};
export type Claim = {
  id: string;
  capabilityId: string;
  productId: string | null;
  planId: string | null;
  state: string;
  evidenceId: string;
};
export type Need = { id: string; name: string };
export type NeedMap = { needId: string; capabilityId: string };
export type Evidence = {
  id: string;
  sourceId: string | null;
  verifiedAt: string | null;
  effectiveStatus: string;
  conditions?: string;
};
export type Source = { id: string; publisher: string; url: string };
export type Catalog = {
  products: Product[];
  plans: Plan[];
  offers: Offer[];
  claims: Claim[];
  needs: Need[];
  mappings: NeedMap[];
  evidence: Evidence[];
  sources: Source[];
  version: string;
};
export type Price = {
  amount: string;
  currency: string;
  regionId: string;
  period: string;
  tier: string;
  sourceUrl: string | null;
  checkedAt: string | null;
  isFree: boolean;
  local: boolean;
};
export type Match = {
  product: Product;
  plan: Plan | null;
  matches: string[];
  unknown: string[];
  prices: Price[];
  score: number;
  budgetFit: 'indicative' | 'not-comparable' | 'above-budget' | 'free';
  planLevelMatches: string[];
};

export const humanGoalNames: Record<string, string> = {
  study: 'Учёба',
  development: 'Программирование',
  research: 'Поиск и анализ',
  presentations: 'Презентации',
  documents: 'Документы',
  writing: 'Тексты',
  images: 'Изображения',
  video: 'Видео',
  editing: 'Монтаж',
  voice: 'Озвучка',
  music: 'Музыка',
  design: 'Дизайн',
  agents: 'AI-агенты',
};
export const presets: { id: string; title: string; needs: string[] }[] = [
  {
    id: 'student',
    title: 'Студент',
    needs: ['study', 'research', 'presentations'],
  },
  {
    id: 'developer',
    title: 'Разработчик',
    needs: ['development', 'agents', 'research'],
  },
  {
    id: 'student-dev',
    title: 'Студент + код',
    needs: ['study', 'development', 'presentations', 'research'],
  },
  {
    id: 'designer',
    title: 'Дизайн',
    needs: ['design', 'images', 'presentations'],
  },
  {
    id: 'creator',
    title: 'Контент',
    needs: ['images', 'video', 'editing', 'voice'],
  },
  {
    id: 'personal',
    title: 'Для себя',
    needs: ['research', 'writing', 'documents'],
  },
];

// Schema money has at most six decimal places. Never compare money as floats.
export function validAmount(value: string): boolean {
  return /^\d{1,12}(?:\.\d{1,6})?$/.test(value);
}
function units(value: string): bigint {
  const [whole, fraction = ''] = value.split('.');
  return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, '0'));
}
function comparePrice(a: Price, b: Price): number {
  // Currencies are grouped alphabetically, never treated as equivalent money.
  if (a.currency !== b.currency) return a.currency.localeCompare(b.currency);
  const av = units(a.amount),
    bv = units(b.amount);
  return av < bv ? -1 : av > bv ? 1 : 0;
}
function isCurrent(from: string, until: string | null, asOf: Date): boolean {
  const now = asOf.getTime();
  return Date.parse(from) <= now && (!until || Date.parse(until) > now);
}
function evidenceOk(e: Evidence | undefined): boolean {
  return e?.effectiveStatus === 'VERIFIED_SOURCE';
}
function priceOf(
  o: Offer,
  p: Plan,
  catalog: Catalog,
  country: string,
  evidences: Map<string, Evidence>,
  asOf: Date,
): Price | null {
  const t = o.terms;
  if (
    !['FREE', 'FIXED'].includes(t.priceKind) ||
    t.amount === null ||
    !['MONTH', 'YEAR'].includes(t.billingPeriod) ||
    !isCurrent(t.validFrom, t.validUntil, asOf)
  )
    return null;
  if (!validAmount(t.amount)) return null;
  // Conditional source can be shown as a list-price observation, never confirmed checkout.
  const e = evidences.get(t.priceEvidenceId);
  if (!e || !['VERIFIED_SOURCE', 'CONDITIONAL'].includes(e.effectiveStatus))
    return null;
  const source = catalog.sources.find((s) => s.id === e.sourceId);
  return {
    amount: t.amount,
    currency: t.currency,
    regionId: t.regionId,
    period: t.billingPeriod,
    tier: p.tier,
    sourceUrl: source && /^https:\/\//.test(source.url) ? source.url : null,
    checkedAt: e.verifiedAt,
    isFree: t.priceKind === 'FREE' && units(t.amount) === 0n,
    local: t.regionId === country.toLowerCase(),
  };
}
function getClaims(
  catalog: Catalog,
  plan: Plan,
  capability: string,
  evidenceMap: Map<string, Evidence>,
): { confirmed: boolean; tierSpecific: boolean } {
  let matches = catalog.claims.filter(
    (a) => a.capabilityId === capability && a.planId === plan.id,
  );
  const tierSpecific = matches.length > 0;
  if (!tierSpecific)
    matches = catalog.claims.filter(
      (a) =>
        a.capabilityId === capability &&
        a.productId === plan.productId &&
        a.planId === null,
    );
  // Conflicting verified claims cannot confirm coverage.
  const proven = matches.filter((a) =>
    evidenceOk(evidenceMap.get(a.evidenceId)),
  );
  const states = new Set(proven.map((a) => a.state));
  return {
    confirmed: states.size === 1 && states.has('SUPPORTED'),
    tierSpecific,
  };
}
function scorePlan(
  catalog: Catalog,
  p: Plan,
  goals: string[],
  evidenceMap: Map<string, Evidence>,
) {
  const matches: string[] = [],
    tier: string[] = [],
    unknown: string[] = [];
  for (const need of goals) {
    const capabilities = catalog.mappings
      .filter((m) => m.needId === need)
      .map((m) => m.capabilityId);
    const claims = capabilities.map((id) =>
      getClaims(catalog, p, id, evidenceMap),
    );
    if (capabilities.length > 0 && claims.every((x) => x.confirmed)) {
      matches.push(need);
      if (claims.every((x) => x.tierSpecific)) tier.push(need);
    } else unknown.push(need);
  }
  return { matches, tier, unknown };
}
function pricesFor(
  plan: Plan,
  catalog: Catalog,
  country: string,
  evidenceMap: Map<string, Evidence>,
  asOf: Date,
): Price[] {
  const offers = catalog.offers.filter((o) => o.terms.planId === plan.id);
  return offers
    .map((o) => priceOf(o, plan, catalog, country, evidenceMap, asOf))
    .filter((p): p is Price => p !== null);
}
function budgetClassification(
  prices: Price[],
  budget: string | null,
  currency: string,
): Match['budgetFit'] {
  if (budget === null || !validAmount(budget)) return 'not-comparable';
  const monthly = prices.filter(
    (p) => p.period === 'MONTH' && p.currency === currency,
  );
  const local = monthly.filter((p) => p.local);
  if (local.some((p) => p.isFree)) return 'free';
  // Known public price can be compared numerically as a *list-price orientation*,
  // never as confirmed local checkout. Conversion between currencies is forbidden.
  const observations = local.length ? local : monthly;
  if (observations.some((p) => units(p.amount) <= units(budget)))
    return 'indicative';
  if (
    observations.length &&
    observations.every((p) => units(p.amount) > units(budget))
  )
    return 'above-budget';
  return 'not-comparable';
}
export function quickMatches(
  catalog: Catalog,
  goals: string[],
  budget: string | null,
  currency: string,
  country: string,
  asOf = new Date(),
): Match[] {
  const evidenceMap = new Map(catalog.evidence.map((e) => [e.id, e]));
  return catalog.products
    .filter((p) => p.state === 'ACTIVE')
    .map((product) => {
      const plans = catalog.plans.filter(
        (p) =>
          p.productId === product.id &&
          p.accessType !== 'API' &&
          isCurrent(p.activeFrom, p.activeUntil, asOf),
      );
      const candidates = plans
        .map((plan) => {
          const cov = scorePlan(catalog, plan, goals, evidenceMap);
          const prices = pricesFor(plan, catalog, country, evidenceMap, asOf);
          const fit = budgetClassification(prices, budget, currency);
          // Internal relevance heuristic only: verified capability coverage, not model quality.
          const score =
            cov.matches.length * 100 +
            cov.tier.length * 3 +
            (fit === 'free' ? 6 : fit === 'indicative' ? 4 : 0);
          return {
            plan,
            matches: cov.matches,
            unknown: cov.unknown,
            prices,
            tier: cov.tier,
            score,
            fit,
          };
        })
        .sort(
          (a, b) => b.score - a.score || a.plan.id.localeCompare(b.plan.id),
        );
      const best = candidates[0];
      return {
        product,
        plan: best?.plan ?? null,
        matches: best?.matches ?? [],
        unknown: best?.unknown ?? goals.slice(),
        prices: best?.prices ?? [],
        score: best?.score ?? 0,
        budgetFit: best?.fit ?? 'not-comparable',
        planLevelMatches: best?.tier ?? [],
      } as Match;
    })
    .sort(
      (a, b) =>
        b.matches.length - a.matches.length ||
        b.score - a.score ||
        a.product.name.localeCompare(b.product.name),
    );
}
export function pickBundle(
  matches: Match[],
  goals: string[],
  limit = 4,
): { items: Match[]; missing: string[] } {
  const remaining = new Set(goals),
    chosen: Match[] = [];
  while (remaining.size > 0 && chosen.length < limit) {
    const next = matches
      .filter((m) => !chosen.some((c) => c.product.id === m.product.id))
      .map((m) => ({ m, cover: m.matches.filter((n) => remaining.has(n)) }))
      .sort(
        (a, b) => b.cover.length - a.cover.length || b.m.score - a.m.score,
      )[0];
    if (!next || next.cover.length === 0) break;
    chosen.push(next.m);
    next.cover.forEach((n) => remaining.delete(n));
  }
  return { items: chosen, missing: [...remaining] };
}
export function pricePresentation(
  match: Match,
  country: string,
  currency?: string,
): { headline: string; detail: string; price: Price | null } {
  const local = match.prices.filter((p) => p.local);
  const regional = local.length ? local : match.prices;
  const preferred = regional.filter((p) => p.currency === currency);
  const choices = preferred.length ? preferred : regional;
  const free = choices.find((p) => p.isFree && p.period === 'MONTH');
  const monthly = choices
    .filter((p) => !p.isFree && p.period === 'MONTH')
    .sort(comparePrice)[0];
  const yearly = choices
    .filter((p) => !p.isFree && p.period === 'YEAR')
    .sort(comparePrice)[0];
  const price = free || monthly || yearly || null;
  if (!price)
    return {
      headline: 'Цена не подтверждена',
      detail: 'Посмотри условия на сайте сервиса',
      price: null,
    };
  const amount = price.amount.replace('.', ',');
  const symbol =
    price.currency === 'USD'
      ? '$'
      : price.currency === 'KZT'
        ? '₸'
        : price.currency === 'CNY'
          ? '¥'
          : price.currency + ' ';
  const formatted =
    price.currency === 'USD' || price.currency === 'CNY'
      ? `${symbol}${amount}`
      : `${amount} ${symbol}`;
  const head = price.isFree
    ? 'Есть бесплатный план'
    : `${formatted}/${price.period === 'YEAR' ? 'год' : 'мес.'}`;
  return {
    headline: head,
    detail: `${price.regionId.toUpperCase()} · базовая цена · ${price.period === 'YEAR' ? 'оплата за год' : 'помесячно'}${price.local ? '' : ' · не цена ' + country}`,
    price,
  };
}
