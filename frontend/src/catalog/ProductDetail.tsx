import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router';
import { readDetail, readMeta } from '../api/catalog';
import type { Detail, Evidence, Offer } from '../api/generated/types.gen';
import {
  CatalogLayout,
  DateLabel,
  ErrorState,
  Loading,
  Status,
} from './CatalogLayout';
import styles from './Catalog.module.css';

export function ProductDetail() {
  const { slug = '' } = useParams();
  const [params] = useSearchParams();
  const pinned = params.get('version');
  const metadata = useQuery({
    queryKey: ['catalog', 'meta'],
    queryFn: ({ signal }) => readMeta(signal),
    enabled: !pinned,
  });
  const version = pinned ?? metadata.data?.versionId;
  const result = useQuery({
    queryKey: ['catalog', 'detail', slug, version],
    queryFn: ({ signal }) => readDetail(slug, version!, signal),
    enabled: !!version,
  });
  return (
    <CatalogLayout>
      <Link to="/catalog">← Все продукты</Link>
      {!pinned && metadata.isError ? (
        <ErrorState
          error={metadata.error}
          retry={() => void metadata.refetch()}
        />
      ) : !pinned && metadata.data?.status === 'EMPTY' ? (
        <p role="status">Каталог пока не опубликован.</p>
      ) : result.isPending ? (
        <Loading />
      ) : result.isError ? (
        <ErrorState error={result.error} retry={() => void result.refetch()} />
      ) : (
        <ProductContent
          key={result.data.versionId + result.data.product.id}
          data={result.data}
        />
      )}
    </CatalogLayout>
  );
}

function EvidenceLink({ id, evidence }: { id: string; evidence: Evidence[] }) {
  const value = evidence.find((e) => e.id === id);
  return (
    <a href={`#evidence-${id}`} className={styles.evidenceLink}>
      <Status value={value?.effectiveStatus ?? 'UNKNOWN'} /> · Источник и дата
    </a>
  );
}

function OfferCard({ offer, data }: { offer: Offer; data: Detail }) {
  const t = offer.terms;
  const period = {
    MONTH: 'месяц',
    YEAR: 'год',
    ONE_TIME: 'однократно',
    USAGE: t.usageUnit ?? 'единицу использования',
  }[t.billingPeriod];
  const region = data.regions.find((r) => r.id === t.regionId);
  return (
    <section className={styles.offer} aria-label={`Предложение ${t.id}`}>
      <h4>
        {region?.name ?? t.regionId} · {t.currency} · {t.channel}
      </h4>
      {t.priceKind === 'UNKNOWN' || t.amount === null ? (
        <p className={styles.price}>Цена неизвестна (UNKNOWN)</p>
      ) : (
        <>
          {t.priceKind === 'FREE' && (
            <p className={styles.small}>Бесплатный базовый тариф</p>
          )}
          {t.priceKind === 'TRIAL' && (
            <p>
              Пробный доступ: {t.trialDays ?? 'неизвестно'} дней. Это не
              постоянный бесплатный тариф.
            </p>
          )}
          <p className={styles.price}>
            {t.amount} {t.currency} / {period}
          </p>
        </>
      )}
      <EvidenceLink id={t.priceEvidenceId} evidence={data.evidence} />
      <dl className={styles.facts}>
        <dt>Первый базовый платёж</dt>
        <dd>
          {offer.upfront === null
            ? 'Неизвестно'
            : `${offer.upfront} ${t.currency}`}
        </dd>
        {t.billingPeriod === 'YEAR' && (
          <>
            <dt>Месячный эквивалент</dt>
            <dd>
              {offer.monthlyEquivalent === null
                ? 'Неизвестно'
                : `${offer.monthlyEquivalent} ${t.currency}`}
              ; оплата за год сразу
            </dd>
          </>
        )}
        <dt>Налоги</dt>
        <dd>
          {t.taxStatus === 'UNKNOWN'
            ? 'Неизвестно'
            : t.taxStatus === 'INCLUDED'
              ? 'Включены'
              : t.taxStatus === 'NOT_APPLICABLE'
                ? 'Не применяются'
                : `${t.additionalTax} ${t.currency}`}{' '}
          <EvidenceLink id={t.taxEvidenceId} evidence={data.evidence} />
        </dd>
        <dt>Дополнительные сборы</dt>
        <dd>
          {t.feeStatus === 'UNKNOWN'
            ? 'Неизвестно'
            : `${t.additionalFee} ${t.currency}`}{' '}
          <EvidenceLink id={t.feeEvidenceId} evidence={data.evidence} />
        </dd>
        <dt>Полный первый платёж</dt>
        <dd>
          {offer.allInUpfront === null
            ? 'Неизвестно — базовая цена не равна итоговому checkout'
            : `${offer.allInUpfront} ${t.currency}`}
        </dd>
        <dt>Доступность в регионе</dt>
        <dd>
          <EvidenceLink id={t.regionEvidenceId} evidence={data.evidence} />
        </dd>
        <dt>Период предложения</dt>
        <dd>
          <DateLabel value={t.validFrom} /> —{' '}
          {t.validUntil ? (
            <DateLabel value={t.validUntil} />
          ) : (
            'окончание неизвестно'
          )}
        </dd>
      </dl>
      <p>{t.conditions}</p>
      {data.quotaPools
        .filter((p) => p.offerId === t.id)
        .map((pool) => (
          <div className={styles.limit} key={pool.id}>
            <h5>Общий лимит</h5>
            <p>
              <Status value={pool.state} />{' '}
              {pool.quantity === null ? 'Количество не указано' : pool.quantity}{' '}
              · {pool.unit}
            </p>
            <p>
              Сброс:{' '}
              {pool.resetPeriod === 'UNKNOWN' ? 'неизвестен' : pool.resetPeriod}
              . {pool.conditions}
            </p>
            {pool.expiresAt && (
              <p>
                Истекает: <DateLabel value={pool.expiresAt} />
              </p>
            )}
            <EvidenceLink id={pool.evidenceId} evidence={data.evidence} />
            <ul>
              {data.usageLimits
                .filter((l) => l.poolId === pool.id)
                .map((l) => (
                  <li key={l.id}>
                    {l.mode} · расход на действие:{' '}
                    {l.unitsPerUse ?? 'неизвестно'}
                  </li>
                ))}
            </ul>
            <p className={styles.small}>
              Несколько функций используют один общий лимит, не отдельные
              балансы.
            </p>
          </div>
        ))}
      {data.eligibilityRules
        .filter((r) => r.offerId === t.id)
        .map((r) => (
          <p key={r.id}>
            <Status value={r.state} /> {r.requirement}. {r.conditions}{' '}
            <EvidenceLink id={r.evidenceId} evidence={data.evidence} />
          </p>
        ))}
    </section>
  );
}

function ProductContent({ data }: { data: Detail }) {
  const [region, setRegion] = useState('');
  const offers = data.offers.filter(
    (o) => !region || o.terms.regionId === region,
  );
  return (
    <>
      <p className={styles.eyebrow}>
        {data.product.kind === 'APPLICATION'
          ? 'ПРИЛОЖЕНИЕ'
          : 'ИНСТРУМЕНТ АВТОМАТИЗАЦИИ'}
      </p>
      <h1>{data.product.name}</h1>
      <p className={styles.version}>Версия: {data.versionId}</p>
      <p className={styles.lead}>
        Условия относятся к выбранному снимку. Неизвестные данные не означают
        отсутствие функции или бесплатный доступ.
      </p>
      <EvidenceLink
        id={data.product.metadataEvidenceId}
        evidence={data.evidence}
      />
      {data.evidence.some((e) => e.status === 'DEMO') && (
        <p className={styles.warning}>
          Синтетические данные для проверки приложения.
        </p>
      )}
      <section className={styles.section} aria-labelledby="capabilities-title">
        <h2 id="capabilities-title">Возможности и ограничения</h2>
        {!data.assertions.length && <p>Возможности пока не проверены.</p>}
        <div className={styles.grid}>
          {data.assertions.map((a) => (
            <article className={styles.card} key={a.id}>
              <h3>
                {data.capabilities.find((c) => c.id === a.capabilityId)?.name ??
                  a.capabilityId}
              </h3>
              <p>
                {a.planId
                  ? `Тариф: ${data.plans.find((p) => p.id === a.planId)?.tier ?? a.planId}`
                  : 'Свойство продукта; условия тарифов могут отличаться'}
              </p>
              <Status value={a.state} />
              <p>{a.conditions}</p>
              <EvidenceLink id={a.evidenceId} evidence={data.evidence} />
            </article>
          ))}
        </div>
      </section>
      <section className={styles.section} aria-labelledby="plans-title">
        <h2 id="plans-title">Тарифы и региональные предложения</h2>
        <label className={styles.region}>
          Регион предложения
          <select value={region} onChange={(e) => setRegion(e.target.value)}>
            <option value="">Все регионы</option>
            {data.regions.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name} ({r.countryCode})
              </option>
            ))}
          </select>
        </label>
        <p>
          Наличие цены не подтверждает право покупки в стране. Валюты не
          конвертируются автоматически.
        </p>
        {!data.plans.length && (
          <p>Подтверждённые тарифы отсутствуют; цена неизвестна.</p>
        )}
        {data.plans.map((plan) => (
          <article className={styles.plan} key={plan.id}>
            <h3>{plan.tier}</h3>
            <p>
              Доступ:{' '}
              {plan.accessType === 'API'
                ? 'API, отдельно от приложения'
                : plan.accessType === 'BOTH'
                  ? 'Приложение и API по указанным условиям'
                  : 'Приложение'}
            </p>
            <h4>Включённые модели</h4>
            {data.modelAccess.filter((a) => a.planId === plan.id).length ===
            0 ? (
              <p>Состав моделей неизвестен.</p>
            ) : (
              <ul>
                {data.modelAccess
                  .filter((a) => a.planId === plan.id)
                  .map((a) => (
                    <li key={a.id}>
                      {data.models.find((m) => m.id === a.modelId)?.name ??
                        a.modelId}{' '}
                      · {a.mode} · <Status value={a.state} />
                      <p>{a.conditions}</p>
                      <EvidenceLink
                        id={a.evidenceId}
                        evidence={data.evidence}
                      />
                    </li>
                  ))}
              </ul>
            )}
            <h4>Включённые агенты</h4>
            {data.agentAccess.filter((a) => a.planId === plan.id).length ===
            0 ? (
              <p>Включение агентов не подтверждено.</p>
            ) : (
              <ul>
                {data.agentAccess
                  .filter((a) => a.planId === plan.id)
                  .map((a) => (
                    <li key={a.id}>
                      {data.agents.find((agent) => agent.id === a.agentId)
                        ?.name ?? a.agentId}{' '}
                      · <Status value={a.state} />
                      <p>
                        Включён в тариф, не отдельная покупка. {a.conditions}
                      </p>
                      <EvidenceLink
                        id={a.evidenceId}
                        evidence={data.evidence}
                      />
                    </li>
                  ))}
              </ul>
            )}
            {offers.filter((o) => o.terms.planId === plan.id).length === 0 ? (
              <p>
                Для выбранного региона предложения не проверены; цена
                неизвестна.
              </p>
            ) : (
              <div className={styles.grid}>
                {offers
                  .filter((o) => o.terms.planId === plan.id)
                  .map((o) => (
                    <OfferCard key={o.terms.id} offer={o} data={data} />
                  ))}
              </div>
            )}
          </article>
        ))}
      </section>
      <section className={styles.section} aria-labelledby="sources-title">
        <h2 id="sources-title">Источники и проверенные утверждения</h2>
        {data.evidence.length === 0 && (
          <p>Источники не указаны. Данные неизвестны.</p>
        )}
        {data.evidence.map((e) => {
          const source = data.sources.find((s) => s.id === e.sourceId);
          return (
            <article
              className={styles.evidence}
              id={`evidence-${e.id}`}
              key={e.id}
            >
              <Status value={e.effectiveStatus} />
              <p>{e.summary}</p>
              <p>{e.conditions}</p>
              <p>
                Проверка: <DateLabel value={e.verifiedAt} />
              </p>
              {source ? (
                <a href={source.url} target="_blank" rel="noopener noreferrer">
                  {source.publisher} — {source.url}
                </a>
              ) : (
                <p>Подтверждающий источник отсутствует.</p>
              )}
            </article>
          );
        })}
      </section>
    </>
  );
}
