import type { Detail, Evidence, Offer } from '../api/generated/types.gen';

export function basePrice(
  amount: string | null,
  currency: string,
  period: string,
) {
  if (amount === null) return 'Цена не подтверждена';
  const unit =
    {
      MONTH: 'мес.',
      YEAR: 'год',
      ONE_TIME: 'однократно',
      USAGE: 'за использование',
    }[period] ?? period;
  return `${amount} ${currency} / ${unit}`;
}
export function supportedEvidence(e: Evidence | undefined) {
  return !!e && ['VERIFIED_SOURCE', 'CONDITIONAL'].includes(e.effectiveStatus);
}
// Decimal comparison without floating-point money or currency conversion.
function units(amount: string) {
  const [whole, fraction = ''] = amount.split('.');
  return BigInt(whole) * 1_000_000n + BigInt(fraction.padEnd(6, '0'));
}
export function cardPrices(
  data: Detail,
  filter: { regionId?: string; currency?: string; priceKind?: string },
  now: number,
) {
  const eligible = data.offers.filter(
    ({ terms: t }) =>
      (!filter.regionId || t.regionId === filter.regionId) &&
      (!filter.currency || t.currency === filter.currency) &&
      (!filter.priceKind || t.priceKind === filter.priceKind) &&
      ['FREE', 'FIXED'].includes(t.priceKind) &&
      t.amount !== null &&
      ['MONTH', 'YEAR', 'ONE_TIME'].includes(t.billingPeriod) &&
      Date.parse(t.validFrom) <= now &&
      (!t.validUntil || Date.parse(t.validUntil) > now) &&
      supportedEvidence(data.evidence.find((e) => e.id === t.priceEvidenceId)),
  );
  const groups = new Map<string, Offer>();
  for (const offer of eligible) {
    const key = `${offer.terms.currency}:${offer.terms.billingPeriod}`;
    const previous = groups.get(key);
    if (!previous || units(offer.terms.amount!) < units(previous.terms.amount!))
      groups.set(key, offer);
  }
  return {
    offers: [...groups.values()].filter(
      (o) =>
        o.terms.billingPeriod !== 'YEAR' ||
        !groups.has(`${o.terms.currency}:MONTH`),
    ),
    hasPaid: eligible.some((o) => o.terms.priceKind === 'FIXED'),
  };
}
