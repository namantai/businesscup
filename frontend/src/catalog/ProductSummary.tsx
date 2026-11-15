import { useQuery } from '@tanstack/react-query';
import { readDetail } from '../api/catalog';
import { DateLabel } from './CatalogLayout';
import { basePrice, cardPrices, supportedEvidence } from './pricing';
import styles from './Catalog.module.css';

export function ProductSummary({
  slug,
  version,
  regionId,
  currency,
  priceKind,
}: {
  slug: string;
  version: string;
  regionId: string;
  currency: string;
  priceKind: string;
}) {
  const result = useQuery({
    queryKey: ['catalog', 'detail', slug, version],
    queryFn: ({ signal }) => readDetail(slug, version, signal),
    staleTime: 60_000,
  });
  if (result.isPending)
    return <p className={styles.small}>Загружаем цены и возможности…</p>;
  if (result.isError)
    return (
      <div>
        <p>Цены не загрузились.</p>
        <button onClick={() => void result.refetch()}>
          Повторить загрузку цены
        </button>
      </div>
    );
  const data = result.data;
  const prices = cardPrices(
    data,
    { regionId, currency, priceKind },
    result.dataUpdatedAt,
  );
  const capabilities = [
    ...new Set(
      data.assertions
        .filter(
          (a) =>
            a.state === 'SUPPORTED' &&
            supportedEvidence(data.evidence.find((e) => e.id === a.evidenceId)),
        )
        .map(
          (a) => data.capabilities.find((c) => c.id === a.capabilityId)?.name,
        )
        .filter(Boolean),
    ),
  ];
  const identity = data.evidence.find(
    (e) => e.id === data.product.metadataEvidenceId,
  );
  const identitySource = data.sources.find((s) => s.id === identity?.sourceId);
  return (
    <div className={styles.productSummary}>
      <p className={styles.purpose}>
        {capabilities.slice(0, 3).join(' · ') ||
          'Возможности пока не подтверждены'}
      </p>
      {capabilities.length > 0 && (
        <p className={styles.small}>Возможности зависят от тарифа.</p>
      )}
      {!prices.offers.length && (
        <>
          <p className={styles.price}>Цена не подтверждена</p>
          <p className={styles.small}>
            Для выбранных условий нет актуальной базовой цены. UNKNOWN не
            означает бесплатно.
          </p>
          {identitySource && /^https?:\/\//.test(identitySource.url) && (
            <p className={styles.small}>
              <a href={identitySource.url} target="_blank" rel="noreferrer">
                Источник о продукте
              </a>{' '}
              · <DateLabel value={identity?.verifiedAt ?? null} />
            </p>
          )}
        </>
      )}
      {prices.offers.map(({ terms: t }) => {
        const evidence = data.evidence.find((e) => e.id === t.priceEvidenceId);
        const source = data.sources.find((s) => s.id === evidence?.sourceId);
        return (
          <div className={styles.priceObservation} key={t.id}>
            <p className={styles.price}>
              {t.priceKind === 'FREE'
                ? prices.hasPaid
                  ? 'Бесплатно + платные тарифы'
                  : 'Бесплатный базовый тариф'
                : `От ${basePrice(t.amount, t.currency, t.billingPeriod)}`}
            </p>
            <p className={styles.small}>
              {data.regions.find((r) => r.id === t.regionId)?.name ??
                t.regionId}{' '}
              · {t.currency}
              {t.billingPeriod === 'YEAR' && ' · Оплата за год сразу'}
            </p>
            <p className={styles.small}>
              {source && /^https?:\/\//.test(source.url) ? (
                <a href={source.url} target="_blank" rel="noreferrer">
                  Источник цены
                </a>
              ) : (
                'Источник не указан'
              )}{' '}
              · <DateLabel value={evidence?.verifiedAt ?? null} />
            </p>
          </div>
        );
      })}
      {!!prices.offers.length && (
        <p className={styles.small}>
          Базовая цена. Налоги, лимиты и доступность оплаты проверяйте отдельно.
        </p>
      )}
    </div>
  );
}
