import { useEffect, useRef } from 'react';
import { Link } from 'react-router';
import type { Bundle, Reason } from '../api/generated/types.gen';
import { CatalogLayout, DateLabel, Status } from '../catalog/CatalogLayout';
import { useBuilder, type CompletedPreview } from './BuilderSession';
import styles from './Builder.module.css';
import { basePrice } from '../catalog/pricing';
import { SavePanel } from '../account/SavePanel';
const strategy = {
  ECONOMY: 'Экономный набор',
  BALANCED: 'Оптимальный набор',
  EXPANDED: 'Расширенный набор',
};
const reasons: Record<Reason['code'], string> = {
  WITHIN_BUDGET: 'Подтверждённые расходы укладываются в бюджет',
  PRICE_INCOMPLETE: 'Цена, налог или комиссия не подтверждены',
  ANNUAL_UPFRONT_REQUIRED: 'Годовой тариф требует предоплаты',
  STUDENT_VERIFICATION_REQUIRED:
    'Нужна проверка студенческого статуса у провайдера',
  REGION_UNKNOWN: 'Покупка в вашей стране не подтверждена',
  REGION_INELIGIBLE: 'Предложение недоступно в выбранном регионе',
  MISSING_REQUIRED_FEATURE: 'Обязательная возможность не подтверждена',
  SHARED_QUOTA: 'Несколько инструментов расходуют общий лимит',
  TRIAL_NOT_RECURRING:
    'Пробный доступ не является постоянным бесплатным тарифом',
  KEEP_EXISTING: 'Сохраняем текущую подписку и учитываем её стоимость',
  INCLUDED_AGENT: 'Агент включён в тариф и не покупается отдельно',
  INCLUDED_MODEL: 'Модель доступна через выбранный тариф',
  UNKNOWN_QUALITY: 'Качество выполнения задач не оценивалось',
  STALE_EVIDENCE: 'Сведения устарели и требуют проверки',
  BUDGET_CONFLICT: 'Расходы превышают месячный бюджет',
  UPFRONT_CONFLICT: 'Недостаточно доступной предоплаты',
  NO_DISTINCT_ALTERNATIVE:
    'Другие стратегии не дают отдельного полезного набора',
  LIMITED_CATALOG: 'Каталог ограничен: это не сравнение всех продуктов рынка',
  SEARCH_TRUNCATED:
    'Поиск ограничен; глобальная оптимальность не гарантируется',
  NO_PUBLISHED_CATALOG: 'Нет опубликованного каталога',
  ELIGIBILITY_UNKNOWN: 'Право на предложение не подтверждено',
  PLATFORM_UNKNOWN: 'Поддержка выбранной платформы не подтверждена',
  PRIVACY_UNKNOWN: 'Условия конфиденциальности не подтверждены',
  LANGUAGE_UNKNOWN: 'Поддержка выбранного языка не подтверждена',
  USAGE_UNKNOWN: 'Достаточность лимитов неизвестна',
  USAGE_EXCEEDED: 'Указанный объём превышает лимит',
  FX_MISSING: 'Нет подтверждённого курса для сравнения валют',
  CONDITION_ACCEPTED: 'Условие явно принято в профиле',
  CONDITIONAL_EVIDENCE: 'Возможность доступна только при условиях',
  CONFLICTED_EVIDENCE: 'Источники расходятся',
  RETAINED_CONFLICT: 'Сохраняемые подписки конфликтуют между собой',
  INCOMPATIBLE_PLAN: 'Тарифы несовместимы в одном наборе',
  NOT_ACTIVE: 'Предложение не действует на дату расчёта',
  PURCHASE_CHANNEL_MISMATCH: 'Предложение относится к другому способу покупки',
  NO_MATCH: 'Подходящий набор не найден',
  BASELINE_INCOMPLETE:
    'Для расчёта экономии недостаточно данных о текущих расходах',
  USER_SUPPLIED_COST: 'Стоимость текущей подписки указана пользователем',
  CANCEL_AT_RENEWAL:
    'Можно пересмотреть подписку при продлении; отмена не выполняется',
  COMPASS_FEE: 'В полном бюджете учтена применимая плата Compass',
  UNKNOWN_CAPABILITY: 'Возможность не описана в каталоге',
  API_NOT_APPLICATION: 'API-доступ не заменяет подписку на приложение',
};
const money = (amount: string | null, currency: string) =>
  amount === null ? 'Цена не подтверждена' : `${amount} ${currency}`;
const action: Record<string, string> = {
  KEEP: 'Сохранить',
  ADD: 'Добавить',
  CANCEL_AT_RENEWAL: 'Пересмотреть при продлении',
  REVIEW: 'Проверить подписку',
};
function ReasonList({ rows }: { rows: Reason[] }) {
  return (
    <ul className={styles.list}>
      {rows.map((r, i) => (
        <li key={i}>
          {reasons[r.code]}
          <details>
            <summary>Основание</summary>
            {r.detail}
            {r.evidenceIds.length > 0 && (
              <p>Свидетельства: {r.evidenceIds.join(', ')}</p>
            )}
          </details>
        </li>
      ))}
    </ul>
  );
}
function EvidenceList({
  completed,
  ids,
}: {
  completed: CompletedPreview;
  ids?: string[];
}) {
  const evidence = ids
    ? completed.response.evidence.filter((e) => ids.includes(e.id))
    : completed.response.evidence;
  return (
    <details className={styles.sources}>
      <summary>Источники и даты проверки ({evidence.length})</summary>
      {!evidence.length && <p>Подтверждающие источники не представлены.</p>}
      <ul className={styles.list}>
        {evidence.map((e) => {
          const source = completed.response.sources.find(
            (s) => s.id === e.sourceId,
          );
          return (
            <li key={e.id}>
              <strong>{e.summary}</strong>
              <br />
              <Status value={e.effectiveStatus} /> ·{' '}
              <DateLabel value={e.verifiedAt} />
              <p>
                {source && /^https?:\/\//.test(source.url) ? (
                  <a href={source.url} target="_blank" rel="noreferrer">
                    {source.publisher} ↗
                  </a>
                ) : (
                  'Первоисточник не указан'
                )}{' '}
                · {e.id}
              </p>
              {e.conditions && <p>Условия: {e.conditions}</p>}
            </li>
          );
        })}
      </ul>
    </details>
  );
}
function BundleCard({
  bundle: b,
  completed,
}: {
  bundle: Bundle;
  completed: CompletedPreview;
}) {
  const { catalog, profile } = completed;
  const c = b.costs;
  // Conditional alternatives may intentionally have no ranked strategy label.
  const title = b.strategies.length
    ? b.strategies.map((s) => strategy[s]).join(' · ')
    : 'Требует проверки: ' +
      b.items
        .map((item) => {
          const product = catalog.products.find((p) => p.id === item.productId);
          const plan = catalog.plans.find((p) => p.id === item.planId);
          return product
            ? product.name + (plan ? ` / ${plan.tier}` : '')
            : 'текущая подписка';
        })
        .join(' + ');
  const capName = (id: string) =>
    catalog.capabilities.find((x) => x.id === id)?.name ??
    `Неописанная возможность (${id})`;
  const isFree =
    c.confirmedBudgetFit &&
    c.confidence === 'CONFIRMED_COMPONENTS' &&
    [c.recurringMonthly, c.upfront, c.horizonTotal].every(
      (v) => v !== null && /^0(?:\.0+)?$/.test(v),
    );
  const evidenceIds = [
    ...new Set([
      ...b.items.flatMap((i) => i.evidenceIds),
      ...b.coverage.flatMap((v) => v.evidenceIds),
      ...b.factualReasons.flatMap((r) => r.evidenceIds),
      ...b.policyReasons.flatMap((r) => r.evidenceIds),
    ]),
  ];
  return (
    <article
      className={styles.card}
      aria-label={title}
      data-testid="bundle-card"
    >
      <div className={styles.tags}>
        <Status value={b.eligibility} />
        {isFree && (
          <span className={styles.tag}>
            FREE · Бесплатный набор по подтверждённым данным
          </span>
        )}
      </div>
      <h2>{title}</h2>
      <h3>Состав набора</h3>
      {!b.items.length && <p>Новые инструменты не требуются.</p>}
      <ul className={styles.list}>
        {b.items.map((item) => {
          const p = catalog.products.find((p) => p.id === item.productId);
          const plan = catalog.plans.find((p) => p.id === item.planId);
          const offer = catalog.offers.find((o) => o.id === item.offerId);
          const priceEvidence = completed.response.evidence.find(
            (e) => e.id === offer?.priceEvidenceId,
          );
          const priceSource = completed.response.sources.find(
            (s) => s.id === priceEvidence?.sourceId,
          );
          return (
            <li key={item.id}>
              <strong>
                {action[item.action] ?? item.action}:{' '}
                {p ? (
                  <Link to={`/catalog/${p.slug}?version=${catalog.version}`}>
                    {p.name}
                  </Link>
                ) : (
                  'Текущая подписка вне каталога'
                )}
                {plan && ` / ${plan.tier}`}
              </strong>
              <div className={styles.itemPrice}>
                {item.originalCharge !== null
                  ? `${item.action === 'ADD' ? 'От ' : ''}${basePrice(item.originalCharge, item.originalCurrency, item.billingPeriod)}`
                  : 'Цена не подтверждена'}
              </div>
              <p className={styles.muted}>
                {item.action === 'KEEP'
                  ? 'Стоимость текущей подписки указана вами.'
                  : 'Опубликованная базовая цена. Налоги, комиссии и доступность оплаты проверяйте отдельно.'}
                {item.billingPeriod === 'YEAR' &&
                  ' Оплата за год сразу, не помесячно.'}
              </p>
              {item.action !== 'KEEP' && priceEvidence && (
                <p className={styles.priceSource}>
                  {priceSource && /^https?:\/\//.test(priceSource.url) ? (
                    <a href={priceSource.url} target="_blank" rel="noreferrer">
                      Источник цены
                    </a>
                  ) : (
                    'Источник не указан'
                  )}{' '}
                  · <DateLabel value={priceEvidence.verifiedAt} /> ·{' '}
                  <Status value={priceEvidence.effectiveStatus} />
                </p>
              )}
              {item.nextRenewal && <p>Продление: {item.nextRenewal}</p>}
              <details>
                <summary>Модели, агенты и общие лимиты</summary>
                <p>
                  Модели:{' '}
                  {item.modelIds.length
                    ? item.modelIds
                        .map(
                          (id) =>
                            catalog.models.find((m) => m.id === id)?.name ??
                            `Неизвестная модель (${id})`,
                        )
                        .join(', ')
                    : 'Доступ не подтверждён'}
                </p>
                <p>
                  Включённые агенты:{' '}
                  {item.includedAgentIds.length
                    ? item.includedAgentIds
                        .map(
                          (id) =>
                            catalog.agents.find((a) => a.id === id)?.name ??
                            `Неизвестный агент (${id})`,
                        )
                        .join(', ')
                    : 'Доступ не подтверждён'}
                </p>
                {!!item.sharedPoolIds.length && (
                  <p>
                    Общий лимит: {item.sharedPoolIds.join(', ')}. Включённые
                    агенты не являются отдельными покупками.
                  </p>
                )}
              </details>
            </li>
          );
        })}
      </ul>
      {!c.confirmedBudgetFit && (
        <p className={styles.notice}>
          Полная стоимость или соответствие бюджету не подтверждены. Этот набор
          нельзя считать гарантированно доступным.
        </p>
      )}
      <h3>Почему подходит</h3>
      <p>
        {b.coverage
          .filter((v) => v.confirmed)
          .map((v) => capName(v.capabilityId))
          .join(' · ') || 'Подтверждённых возможностей пока недостаточно.'}
      </p>
      <p className={styles.muted}>
        Состав выбран под ваши цели и ограничения. Это сопоставление
        возможностей, не рейтинг качества.
      </p>
      <details className={styles.sources}>
        <summary>Покрытие целей и лимиты</summary>
        <p>
          Выбраны:{' '}
          {profile.needs
            .map((n) => catalog.needs.find((c) => c.id === n.id)?.name ?? n.id)
            .join(', ')}
          . Соответствие ниже показано по возможностям каталога; это не оценка
          качества в процентах.
        </p>
        <ul className={styles.list}>
          {b.coverage.map((v) => (
            <li key={v.capabilityId}>
              {capName(v.capabilityId)} — <Status value={v.state} /> ·{' '}
              {v.required ? 'обязательное' : 'желательное'} ·{' '}
              {v.confirmed ? 'подтверждено' : 'не подтверждено'}
              {!v.acceptedForRequirement && ' · требование не закрыто'}
            </li>
          ))}
        </ul>
        <p>
          Достаточность лимитов:{' '}
          {b.usageFit === 'UNKNOWN'
            ? 'неизвестна'
            : b.usageFit === 'CONFIRMED'
              ? 'подтверждена'
              : b.usageFit}
          .
        </p>
      </details>
      <h3>Что важно учесть</h3>
      <ul className={styles.list}>
        {[...new Set(b.factualReasons.map((r) => reasons[r.code]))]
          .slice(0, 3)
          .map((text) => (
            <li key={text}>{text}</li>
          ))}
      </ul>
      <details className={styles.sources}>
        <summary>Все ограничения и основания</summary>
        <ReasonList rows={b.factualReasons} />
      </details>
      {!!b.policyReasons.length && (
        <details className={styles.sources}>
          <summary>Правила сравнения</summary>
          <ReasonList rows={b.policyReasons} />
        </details>
      )}
      <p className={styles.totalPrice}>
        В месяц (эквивалент):{' '}
        <strong>{money(c.recurringMonthly, c.currency)}</strong>
      </p>
      <details className={styles.sources}>
        <summary>Полный расчёт стоимости</summary>
        <dl className={styles.money}>
          {[
            ['К оплате сейчас', c.cashDueNow],
            ['Полная предоплата', c.upfront],
            ['Регулярно в месяц (эквивалент)', c.recurringMonthly],
            [`Всего за ${profile.horizonMonths} мес.`, c.horizonTotal],
          ].map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{money(value, c.currency)}</dd>
            </div>
          ))}
        </dl>
        <p className={styles.muted}>
          Месячный эквивалент — не помесячный платёж. Неизвестные налоги и
          комиссии не равны нулю.
        </p>
        {!c.confirmedBudgetFit && (
          <p className={styles.notice}>
            Известные суммы — нижняя граница расходов. Полная стоимость может
            быть выше.
          </p>
        )}
      </details>
      <details className={styles.sources}>
        <summary>Подробнее о расходах и экономии</summary>
        <dl className={styles.money}>
          {[
            ['В месяц после изменений', c.futureMonthly],
            [
              'Текущие сопоставимые расходы / мес.',
              c.comparableBaselineMonthly,
            ],
            ['Потенциальная разница / мес.', c.potentialNetMonthlySavings],
            ['Плата Compass / мес.', c.compassFeeMonthly],
          ].map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{money(value, c.currency)}</dd>
            </div>
          ))}
          <div>
            <dt>Годовая стоимость</dt>
            <dd>
              {profile.horizonMonths === 12
                ? money(c.horizonTotal, c.currency)
                : 'Выберите период 12 месяцев для годового расчёта'}
            </dd>
          </div>
        </dl>
        <p className={styles.muted}>
          Положительная разница — снижение сопоставимых расходов, отрицательная
          — рост. Применимая плата Compass включена в полный бюджет.
        </p>
      </details>
      {!!b.conditions.length && (
        <>
          <details className={styles.sources}>
            <summary>Условия тарифов полностью</summary>
            <ul className={styles.list}>
              {b.conditions.map((s, i) => (
                <li key={i}>{s}</li>
              ))}
            </ul>
          </details>
        </>
      )}
      {!!b.portfolioActions.length && (
        <>
          <h3>Текущие подписки</h3>
          <ul className={styles.list}>
            {b.portfolioActions.map((p, i) => (
              <li key={i}>
                {action[p.action]} · {p.subscriptionId}: {p.explanation}
              </li>
            ))}
          </ul>
          <p>
            Это рекомендации. Подписки не покупаются и не отменяются
            автоматически.
          </p>
        </>
      )}
      <EvidenceList completed={completed} ids={evidenceIds} />
    </article>
  );
}
export function ResultsPage() {
  const { completed } = useBuilder();
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  return (
    <CatalogLayout>
      <div className={styles.heading}>
        <p className={styles.eyebrow}>ВАШ AI STACK</p>
        <h1 tabIndex={-1} ref={heading}>
          Результаты подбора
        </h1>
      </div>
      {!completed ? (
        <section className={styles.panel}>
          <h2>Сначала расскажите о потребностях</h2>
          <p>
            В этой вкладке ещё нет актуального результата. После изменения
            параметров нужно выполнить подбор заново.
          </p>
          <Link className={styles.primary} to="/onboarding">
            Перейти к анкете
          </Link>
        </section>
      ) : (
        <>
          <div className={styles.actions}>
            <Link className={styles.primary} to="/onboarding">
              Изменить параметры
            </Link>
            <Link className={styles.secondary} to="/catalog">
              Изучить каталог
            </Link>
          </div>
          <p className={styles.muted}>
            Страна: {completed.profile.country} · Бюджет:{' '}
            {completed.profile.budget.monthlyMax}{' '}
            {completed.profile.budget.currency} / мес. · Период:{' '}
            {completed.profile.horizonMonths} мес.
          </p>
          {completed.response.datasetKind === 'DEMO' && (
            <p className={styles.notice}>
              Тип данных: {completed.response.datasetKind}. Синтетические и
              демонстрационные сведения не являются рыночными фактами.
            </p>
          )}
          {!completed.response.bundles.length && (
            <section className={styles.notice} role="status">
              <h2>
                {completed.response.status === 'CONDITIONAL_ONLY'
                  ? 'Есть варианты, но условия нужно проверить.'
                  : 'Пока не нашли подходящий вариант.'}
              </h2>
              <p>
                Попробуй изменить бюджет или требования. Непокрытые цели и
                неизвестные данные показаны ниже.
              </p>
            </section>
          )}
          {!!completed.response.warnings.length && (
            <section className={styles.notice}>
              <details className={styles.sources}>
                <summary>О поиске и ограничениях каталога</summary>
                <ReasonList rows={completed.response.warnings} />
              </details>
            </section>
          )}
          {completed.response.bundles.map((b) => (
            <BundleCard key={b.id} bundle={b} completed={completed} />
          ))}
          {!!completed.response.conditionalAlternatives.length && (
            <section aria-label="Условные варианты">
              <h2>Варианты при дополнительных условиях</h2>
              <p className={styles.notice}>
                Эти наборы не гарантируют доступность или соблюдение бюджета.
                Сначала проверьте цены, регион и право на льготу.
              </p>
              {completed.response.conditionalAlternatives.map((b) => (
                <BundleCard key={b.id} bundle={b} completed={completed} />
              ))}
            </section>
          )}
          {!!completed.response.unmetRequirements.length && (
            <section className={styles.panel}>
              <h2>Непокрытые требования</h2>
              <ul className={styles.list}>
                {completed.response.unmetRequirements.map((r, i) => (
                  <li key={i}>
                    {completed.catalog.capabilities.find(
                      (c) => c.id === r.capabilityId,
                    )?.name ?? r.capabilityId}
                    : <Status value={r.state} /> · {reasons[r.reason]}
                  </li>
                ))}
              </ul>
            </section>
          )}
          <details className={styles.sources}>
            <summary>Почему другие предложения не подошли</summary>
            {completed.response.assessments.map((a) => (
              <section key={a.id}>
                <h3>
                  {completed.catalog.plans.find(
                    (p) =>
                      p.id ===
                      completed.catalog.offers.find((o) => o.id === a.id)
                        ?.planId,
                  )?.tier ?? a.id}{' '}
                  — <Status value={a.eligibility} />
                </h3>
                <ReasonList rows={a.reasons} />
              </section>
            ))}
          </details>
          <SavePanel key={completed.response.requestId} />
          <EvidenceList completed={completed} />
          <details className={styles.sources}>
            <summary>Данные расчёта</summary>
            <p>
              Каталог: {completed.response.catalogVersion} · Движок:{' '}
              {completed.response.engineVersion} · Правила:{' '}
              {completed.response.policyVersion}
            </p>
            <p>
              Дата расчёта: {completed.response.asOf} · Идентификатор:{' '}
              {completed.response.requestId}
            </p>
            <p>
              Поиск{' '}
              {completed.response.searchComplete ? 'завершён' : 'ограничен'}:{' '}
              {completed.response.search.limitation ??
                'без ограничения перебора'}
            </p>
            <p>
              Курсы валют:{' '}
              {completed.response.fxSnapshots.length
                ? completed.response.fxSnapshots
                    .map((f) => JSON.stringify(f))
                    .join('; ')
                : 'Не использовались. Без подтверждённого курса сравнение разных валют недоступно.'}
            </p>
          </details>
        </>
      )}
    </CatalogLayout>
  );
}
