import { emptySubscription } from './form';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { CatalogLayout, Status } from '../catalog/CatalogLayout';
import { PersonalGate, PersonalError } from '../account/PersonalGate';
import { useAuth } from '../account/AuthSession';
import { useBuilder } from '../builder/BuilderSession';
import {
  checked,
  protection,
  subscriptions,
  createSubscription,
  updateSubscription,
  deleteSubscription,
  analyzeSubscriptions,
  profiles,
  meta,
  evidence,
} from '../api/account';
import { readBuilderCatalog, type BuilderCatalog } from '../api/builder';
import type {
  SubscriptionTerms,
  SubscriptionEntry,
  OptimizationResult,
  Scenario,
  RecommendationProfile,
  Evidence,
} from '../api/generated/types.gen';
import styles from '../builder/Builder.module.css';

const money = (amount: string | null, currency: string) =>
  amount === null ? 'UNKNOWN — неизвестно' : `${amount} ${currency}`;
export function SubscriptionPage() {
  const { account } = useAuth();
  return (
    <CatalogLayout>
      <h1>Мои AI-подписки</h1>
      <p>
        Ваши расходы, будущие продления и варианты изменения набора. Без
        подключения банка.
      </p>
      <PersonalGate>
        {account && <Ledger key={account.id} owner={account.id} />}
      </PersonalGate>
    </CatalogLayout>
  );
}
function Ledger({ owner }: { owner: string }) {
  const builder = useBuilder();
  const [editing, setEditing] = useState<SubscriptionEntry | null>(null);
  const [form, setForm] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [pendingKeep, setPendingKeep] = useState<{
    id: string;
    value: boolean;
  } | null>(null);
  const [report, setReport] = useState<OptimizationResult | null>(null);
  const [profileChoice, setProfileChoice] = useState('');
  const [mode, setMode] = useState<'REVIEW' | 'AUGMENT'>('REVIEW');
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  const ledger = useQuery({
    queryKey: ['personal', owner, 'subscriptions'],
    queryFn: ({ signal }) =>
      checked(
        subscriptions({ signal, headers: { 'X-Compass-Account': owner } }),
      ),
  });
  const saved = useQuery({
    queryKey: ['personal', owner, 'optimizer-profiles'],
    queryFn: ({ signal }) =>
      checked(profiles({ signal, headers: { 'X-Compass-Account': owner } })),
  });
  const catalog = useQuery({
    queryKey: ['optimizer-catalog'],
    queryFn: async ({ signal }) => {
      const m = await checked(meta({ signal }));
      if (!m.versionId) return null;
      const [data, facts] = await Promise.all([
        readBuilderCatalog(m.versionId, signal),
        checked(
          evidence({ signal, query: { version: m.versionId, size: 100 } }),
        ),
      ]);
      return { data, facts: facts.items };
    },
  });
  async function action(work: (controller: AbortController) => Promise<void>) {
    if (request.current) return;
    const controller = new AbortController();
    request.current = controller;
    setBusy(true);
    setError(null);
    try {
      await work(controller);
    } catch (e) {
      if (!controller.signal.aborted) setError(e);
    } finally {
      if (!controller.signal.aborted) setBusy(false);
      request.current = null;
    }
  }
  async function save(terms: SubscriptionTerms) {
    await action(async (c) => {
      const opts = {
        ...(await protection(owner)),
        signal: c.signal,
        body: { terms, revision: editing?.revision ?? 0 },
      };
      await checked(
        editing
          ? updateSubscription({ ...opts, path: { id: editing.id } })
          : createSubscription(opts),
      );
      if (c.signal.aborted) return;
      setForm(false);
      setEditing(null);
      setReport(null);
      await ledger.refetch();
    });
  }
  async function remove(entry: SubscriptionEntry) {
    await action(async (c) => {
      await checked(
        deleteSubscription({
          ...(await protection(owner)),
          signal: c.signal,
          path: { id: entry.id },
          query: { revision: entry.revision },
        }),
      );
      if (!c.signal.aborted) {
        setReport(null);
        await ledger.refetch();
      }
    });
  }
  async function toggle(entry: SubscriptionEntry) {
    if (request.current) return;
    setPendingKeep({ id: entry.id, value: !entry.terms.mustKeep });
    setReport(null);
    try {
      await action(async (c) => {
        await checked(
          updateSubscription({
            ...(await protection(owner)),
            signal: c.signal,
            path: { id: entry.id },
            body: {
              revision: entry.revision,
              terms: { ...entry.terms, mustKeep: !entry.terms.mustKeep },
            },
          }),
        );
        if (!c.signal.aborted) {
          setReport(null);
          await ledger.refetch();
        }
      });
    } finally {
      setPendingKeep(null);
    }
  }
  const profile: RecommendationProfile | undefined =
    profileChoice === 'draft'
      ? builder.completed?.profile
      : (saved.data?.find((p) => p.id === profileChoice) ?? saved.data?.[0])
          ?.profile;
  async function compare() {
    if (!profile || !catalog.data || !ledger.data) return;
    await action(async (c) => {
      setReport(null);
      const result = await checked(
        analyzeSubscriptions({
          ...(await protection(owner)),
          signal: c.signal,
          body: {
            profile: {
              ...profile,
              existingSubscriptions: [],
              portfolioMode: mode,
              asOf: null,
            },
            catalogVersion: catalog.data!.data.version,
            revisions: Object.fromEntries(
              ledger.data!.map((e) => [e.id, e.revision]),
            ),
          },
        }),
      );
      if (!c.signal.aborted) setReport(result);
    });
  }
  const failure = error || ledger.error || saved.error || catalog.error;
  return (
    <>
      <p>
        <Link to="/dashboard">Мои профили</Link> ·{' '}
        <Link to="/account">Экспорт и удаление данных</Link>
      </p>
      <p className={styles.muted}>
        Удаление записи в Compass не отменяет подписку у поставщика. Уже
        уплаченные деньги не считаются возвращёнными.
      </p>
      {failure ? (
        <>
          <PersonalError error={failure} />
          <button
            disabled={busy}
            onClick={() => {
              setError(null);
              setReport(null);
              void ledger.refetch();
              void saved.refetch();
              void catalog.refetch();
            }}
          >
            Обновить данные и повторить
          </button>
        </>
      ) : null}
      {ledger.isPending && <p role="status">Загружаем подписки…</p>}
      {ledger.data?.length === 0 && (
        <p>
          Подписок пока нет. Добавьте фактические расходы или личную запись с
          неизвестной ценой.
        </p>
      )}
      <div className={styles.grid}>
        {ledger.data?.map((entry) => {
          const t = entry.terms;
          const plan = catalog.data?.data.plans.find((p) => p.id === t.planId);
          const product = catalog.data?.data.products.find(
            (p) => p.id === plan?.productId,
          );
          return (
            <article className={styles.card} key={entry.id} aria-label={t.name}>
              <h2>{t.name}</h2>
              <p>
                {money(t.amount, t.currency)} /{' '}
                {t.billingPeriod === 'YEAR'
                  ? 'год — полная предоплата'
                  : 'месяц'}
              </p>
              <p>
                Цена:{' '}
                {t.priceOrigin === 'USER_REPORTED'
                  ? 'указана вами (USER_REPORTED)'
                  : t.priceOrigin === 'CATALOG'
                    ? 'из каталога; проверьте актуальность источника'
                    : 'UNKNOWN'}
              </p>
              <p>
                Указанная дата продления: {t.nextRenewal ?? 'UNKNOWN'} (
                {t.billingZone}). Оплачено до: {t.paidUntil ?? 'UNKNOWN'}.
              </p>
              <p>
                Автопродление:{' '}
                {t.autoRenew === 'YES'
                  ? 'включено'
                  : t.autoRenew === 'NO'
                    ? 'отключено'
                    : 'UNKNOWN'}
                . Статус:{' '}
                {t.status === 'ACTIVE'
                  ? 'действует'
                  : t.status === 'TRIAL'
                    ? 'пробный период'
                    : t.status === 'ENDED'
                      ? 'завершена'
                      : 'UNKNOWN'}
                .
              </p>
              <label className={styles.check}>
                <input
                  type="checkbox"
                  checked={
                    pendingKeep?.id === entry.id
                      ? pendingKeep.value
                      : t.mustKeep
                  }
                  disabled={busy}
                  onChange={() => void toggle(entry)}
                />
                Обязательно сохранить
              </label>
              {product && (
                <p>
                  <Link
                    to={`/catalog/${product.slug}?version=${catalog.data!.data.version}`}
                  >
                    Карточка продукта и источники
                  </Link>
                </p>
              )}
              <div className={styles.actions}>
                <button
                  disabled={busy}
                  onClick={() => {
                    setEditing(entry);
                    setForm(true);
                  }}
                >
                  Редактировать
                </button>
                <button disabled={busy} onClick={() => void remove(entry)}>
                  Удалить запись
                </button>
              </div>
            </article>
          );
        })}
      </div>
      <p>
        <button
          className={styles.primary}
          disabled={busy || (ledger.data?.length ?? 0) >= 8}
          onClick={() => {
            setEditing(null);
            setForm(true);
          }}
        >
          Добавить подписку
        </button>
      </p>
      {form && (
        <SubscriptionForm
          key={editing?.id ?? 'new'}
          initial={editing?.terms}
          catalog={catalog.data?.data}
          facts={catalog.data?.facts ?? []}
          busy={busy}
          save={save}
          cancel={() => setForm(false)}
        />
      )}
      <section className={styles.panel} aria-label="Анализ расходов">
        <h2>Сравнить расходы и возможности</h2>
        <p>
          Используем ваши потребности и ограничения из сохранённого профиля или
          последнего подбора. Подписки берём из списка выше.
        </p>
        <label className={styles.field}>
          Профиль потребностей
          <select
            value={profileChoice}
            disabled={busy}
            onChange={(e) => {
              setProfileChoice(e.target.value);
              setReport(null);
            }}
          >
            <option value="">Первый сохранённый профиль</option>
            {saved.data?.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
            {builder.completed && (
              <option value="draft">Последний подбор в этой вкладке</option>
            )}
          </select>
        </label>
        {!profile && (
          <p>
            <Link to="/onboarding">Выберите потребности в анкете</Link>, затем
            сохраните профиль или вернитесь сюда после подбора.
          </p>
        )}
        <label className={styles.field}>
          Что можно менять
          <select
            value={mode}
            disabled={busy}
            onChange={(e) => {
              setMode(e.target.value as typeof mode);
              setReport(null);
            }}
          >
            <option value="REVIEW">
              Пересмотреть, сохранив обязательные подписки
            </option>
            <option value="AUGMENT">Только дополнить существующий набор</option>
          </select>
        </label>
        {profile && (
          <p>
            Горизонт: {profile.horizonMonths} мес. Валюта:{' '}
            {profile.budget.currency}. Бюджет: {profile.budget.monthlyMax},
            предоплата до {profile.budget.upfrontMax}.{' '}
            <Link to="/onboarding">Изменить параметры в анкете</Link>
          </p>
        )}
        {!catalog.isPending && !catalog.data && (
          <p>
            Нет опубликованного каталога. Записи можно хранить; сравнение пока
            недоступно.
          </p>
        )}
        <button
          className={styles.primary}
          disabled={busy || !profile || !catalog.data || !ledger.data?.length}
          onClick={() => void compare()}
        >
          {busy ? 'Выполняем запрос…' : 'Рассчитать и сравнить'}
        </button>
      </section>
      {report && (
        <Comparison
          report={report}
          catalog={catalog.data?.data}
          names={Object.fromEntries(
            (ledger.data ?? []).map((e) => [e.id, e.terms.name]),
          )}
        />
      )}
    </>
  );
}
function SubscriptionForm({
  initial,
  catalog,
  facts,
  busy,
  save,
  cancel,
}: {
  initial?: SubscriptionTerms;
  catalog?: BuilderCatalog;
  facts: Evidence[];
  busy: boolean;
  save: (t: SubscriptionTerms) => Promise<void>;
  cancel: () => void;
}) {
  const [t, set] = useState(initial ?? emptySubscription);
  const change = <K extends keyof SubscriptionTerms>(
    key: K,
    value: SubscriptionTerms[K],
  ) => set((v) => ({ ...v, [key]: value }));
  const field = (
    key:
      | 'amount'
      | 'additionalPerPeriod'
      | 'transitionCost'
      | 'paidAmount'
      | 'renewalAnchor'
      | 'nextRenewal'
      | 'paidFrom'
      | 'paidUntil'
      | 'trialUntil'
      | 'commitmentUntil'
      | 'cancellationAvailableFrom',
    label: string,
    date = false,
  ) => (
    <label className={styles.field}>
      {label}
      <input
        type={date ? 'date' : 'text'}
        inputMode={date ? undefined : 'decimal'}
        value={t[key] ?? ''}
        placeholder="Неизвестно"
        onChange={(e) => {
          const v = e.target.value || null;
          change(key, v);
          if (key === 'amount')
            change('priceOrigin', v === null ? 'UNKNOWN' : 'USER_REPORTED');
        }}
      />
    </label>
  );
  function selectOffer(id: string) {
    const o = catalog?.offers.find((o) => o.id === id);
    if (!o || !catalog) {
      set((v) => ({
        ...v,
        catalogVersion: null,
        planId: null,
        offerId: null,
        priceOrigin: v.amount === null ? 'UNKNOWN' : 'USER_REPORTED',
      }));
      return;
    }
    const price = facts.find((e) => e.id === o.priceEvidenceId);
    const verified =
      price && ['VERIFIED_SOURCE', 'DEMO'].includes(price.effectiveStatus);
    const plan = catalog.plans.find((p) => p.id === o.planId);
    const product = catalog.products.find((p) => p.id === plan?.productId);
    set((v) => ({
      ...v,
      name: `${product?.name ?? ''} ${plan?.tier ?? ''}`.trim(),
      catalogVersion: catalog.version,
      planId: o.planId,
      offerId: o.id,
      currency: o.currency,
      amount: verified ? o.amount : null,
      priceOrigin: verified && o.amount !== null ? 'CATALOG' : 'UNKNOWN',
      billingPeriod: o.billingPeriod === 'YEAR' ? 'YEAR' : 'MONTH',
      additionalPerPeriod:
        o.additionalTax === '0' &&
        o.additionalFee === '0' &&
        [o.taxEvidenceId, o.feeEvidenceId].every((id) =>
          facts.some(
            (e) =>
              e.id === id &&
              ['VERIFIED_SOURCE', 'DEMO'].includes(e.effectiveStatus),
          ),
        )
          ? '0'
          : null,
    }));
  }
  return (
    <form
      className={styles.panel}
      onSubmit={(e) => {
        e.preventDefault();
        void save(t);
      }}
    >
      <fieldset disabled={busy} style={{ border: 0, padding: 0, minWidth: 0 }}>
        <h2>{initial ? 'Редактирование подписки' : 'Новая подписка'}</h2>
        <label className={styles.field}>
          Тариф из каталога
          <select
            value={t.offerId ?? ''}
            onChange={(e) => selectOffer(e.target.value)}
          >
            <option value="">Мой продукт вне каталога</option>
            {catalog?.offers
              .filter(
                (o) =>
                  o.billingPeriod === 'MONTH' || o.billingPeriod === 'YEAR',
              )
              .map((o) => (
                <option key={o.id} value={o.id}>
                  {catalog.plans.find((p) => p.id === o.planId)?.tier} ·{' '}
                  {o.regionId} · {o.currency} · {o.billingPeriod}
                </option>
              ))}
          </select>
        </label>
        <p>
          Подтверждённая цена из каталога заполняется автоматически. Изменённая
          вами сумма — USER_REPORTED. Пустое поле означает UNKNOWN; 0 указывайте
          только если расхода действительно нет.
        </p>
        <div className={styles.grid}>
          <label className={styles.field}>
            Название
            <input
              required
              maxLength={100}
              value={t.name}
              onChange={(e) => change('name', e.target.value)}
            />
          </label>
          <label className={styles.field}>
            Валюта
            <input
              required
              value={t.currency}
              onChange={(e) => {
                change('currency', e.target.value.toUpperCase());
                change(
                  'priceOrigin',
                  t.amount === null ? 'UNKNOWN' : 'USER_REPORTED',
                );
              }}
            />
          </label>
          {field('amount', 'Фактическая цена за период')}
          <label className={styles.field}>
            Период оплаты
            <select
              value={t.billingPeriod}
              onChange={(e) => {
                change(
                  'billingPeriod',
                  e.target.value as SubscriptionTerms['billingPeriod'],
                );
                change(
                  'priceOrigin',
                  t.amount === null ? 'UNKNOWN' : 'USER_REPORTED',
                );
              }}
            >
              <option value="MONTH">Месяц</option>
              <option value="YEAR">Год — полная предоплата</option>
            </select>
          </label>
          {field('renewalAnchor', 'Исходная дата продления', true)}
          {field('nextRenewal', 'Дата следующего списания', true)}
          <label className={styles.field}>
            Часовой пояс списания
            <input
              required
              value={t.billingZone}
              onChange={(e) => change('billingZone', e.target.value)}
            />
          </label>
          <label className={styles.field}>
            Автопродление
            <select
              value={t.autoRenew}
              onChange={(e) =>
                change(
                  'autoRenew',
                  e.target.value as SubscriptionTerms['autoRenew'],
                )
              }
            >
              <option value="UNKNOWN">Неизвестно</option>
              <option value="YES">Включено</option>
              <option value="NO">Отключено</option>
            </select>
          </label>
        </div>
        <p>
          Для ежемесячного списания 31-го сохраните исходную дату с 31-м числом:
          февраль не меняет правило на следующие месяцы.
        </p>
        <details>
          <summary>Оплаченный период, отмена и дополнительные расходы</summary>
          <div className={styles.grid}>
            <label className={styles.field}>
              Статус
              <select
                value={t.status}
                onChange={(e) =>
                  change(
                    'status',
                    e.target.value as SubscriptionTerms['status'],
                  )
                }
              >
                <option value="ACTIVE">Действует</option>
                <option value="TRIAL">Пробный период</option>
                <option value="ENDED">Завершена</option>
                <option value="UNKNOWN">Неизвестно</option>
              </select>
            </label>
            {field('paidFrom', 'Оплачено с', true)}
            {field('paidUntil', 'Оплачено до (не включая дату)', true)}
            {field('paidAmount', 'Уже уплачено, без предполагаемого возврата')}
            {field('trialUntil', 'Пробный период до', true)}
            <label className={styles.field}>
              Обязательство по контракту
              <select
                value={t.obligation}
                onChange={(e) => {
                  change(
                    'obligation',
                    e.target.value as SubscriptionTerms['obligation'],
                  );
                  if (e.target.value !== 'UNTIL')
                    change('commitmentUntil', null);
                }}
              >
                <option value="UNKNOWN">Неизвестно</option>
                <option value="NONE">Нет обязательства</option>
                <option value="UNTIL">До указанной даты</option>
              </select>
            </label>
            {t.obligation === 'UNTIL' &&
              field('commitmentUntil', 'Обязательные платежи до', true)}
            {field(
              'cancellationAvailableFrom',
              'Можно прекратить будущие списания с',
              true,
            )}
            {field(
              'additionalPerPeriod',
              'Дополнительно за период: налоги, сборы, кредиты',
            )}
            {field('transitionCost', 'Расходы на переход / отмену')}
          </div>
          <p>
            Укажите 0 дополнительных расходов, если фактическая цена уже
            включает их. Неизвестный объём платных кредитов оставьте пустым.
            Даты и условия здесь — ваши сведения, не подтверждение провайдера.
          </p>
        </details>
        <label className={styles.check}>
          <input
            type="checkbox"
            checked={t.mustKeep}
            onChange={(e) => change('mustKeep', e.target.checked)}
          />
          Обязательно сохранить эту подписку
        </label>
        <div className={styles.actions}>
          <button className={styles.primary} disabled={busy}>
            Сохранить подписку
          </button>
          <button type="button" disabled={busy} onClick={cancel}>
            Закрыть форму
          </button>
        </div>
      </fieldset>
    </form>
  );
}
export function Comparison({
  report,
  catalog,
  names,
}: {
  report: OptimizationResult;
  catalog?: BuilderCatalog;
  names: Record<string, string>;
}) {
  const decisions: Record<string, string> = {
    KEEP_CURRENT: 'Лучше ничего не менять',
    CONSIDER_CHANGE: 'Есть вариант для рассмотрения',
    UNKNOWN: 'Экономию пока нельзя определить',
    REVIEW_REQUIREMENTS: 'Проверьте потребности и ограничения',
  };
  const actionLabels: Record<string, string> = {
    KEEP: 'Сохранить',
    ADD: 'Добавить',
    CANCEL_AT_RENEWAL: 'Отключить продление у поставщика',
    REVIEW: 'Уточнить условия',
  };
  const overlapLabels: Record<string, string> = {
    CONFIRMED_FUNCTIONAL_OVERLAP: 'Подтверждённое пересечение функций',
    INSUFFICIENT_DATA: 'Недостаточно данных',
    BLOCKED_BY_REQUIREMENTS: 'Замена ограничена обязательными подписками',
    POSSIBLE_ALTERNATIVE: 'Возможная альтернатива',
  };
  const readable = (text: string) =>
    Object.entries(names).reduce(
      (value, [id, name]) => value.replaceAll(id, name),
      text,
    );
  const itemName = (id: string) => {
    const personal = names[id] ?? names[id.replace(/^existing:/, '')];
    if (personal) return personal;
    const item = [report.current, ...report.alternatives]
      .flatMap((s) => s.items)
      .find((i) => i.id === id);
    return (
      catalog?.products.find((p) => p.id === item?.productId)?.name ??
      'Инструмент вне каталога'
    );
  };
  function scenario(s: Scenario) {
    return (
      <article className={styles.card} key={s.id}>
        <h3>{s.label === 'CURRENT' ? 'Текущий набор' : 'Возможный набор'}</h3>
        <dl>
          <dt>Уже оплачено — не экономия</dt>
          <dd>{money(s.costs.paidAmount, report.currency)}</dd>
          <dt>Списать сейчас / предоплата</dt>
          <dd>{money(s.costs.cashDueNow, report.currency)}</dd>
          <dt>Регулярно в месяц (эквивалент)</dt>
          <dd>{money(s.costs.recurringMonthly, report.currency)}</dd>
          <dt>
            Будущие расходы за{' '}
            {report.horizonMonths === 12
              ? 'год'
              : `${report.horizonMonths} мес.`}
          </dt>
          <dd>{money(s.costs.horizonTotal, report.currency)}</dd>
          <dt>Неизбежные платежи за горизонт</dt>
          <dd>{money(s.costs.mandatoryTotal, report.currency)}</dd>
          <dt>Переходные расходы</dt>
          <dd>{money(s.costs.transitionCost, report.currency)}</dd>
        </dl>
        {s.label !== 'CURRENT' && (
          <p>
            Возможная разница: {money(s.potentialSavings, report.currency)}.{' '}
            {s.savingsStatus === 'UNKNOWN'
              ? 'Не хватает данных.'
              : 'По указанным вами условиям, не гарантированная экономия.'}{' '}
            Подтверждённая поставщиком экономия:{' '}
            {money(s.confirmedSavings, report.currency)}.
          </p>
        )}
        <p>
          Обязательные требования:{' '}
          {s.requirementsMet
            ? 'соблюдены по доступным данным'
            : 'не подтверждены'}
          . Записей: {s.subscriptionIds.length}; новых покупок:{' '}
          {s.items.filter((i) => i.action === 'ADD').length}.
        </p>
        <ul>
          {s.actions.map((a) => (
            <li key={a.subscriptionId}>
              {itemName(a.subscriptionId)}: {actionLabels[a.action]} —{' '}
              {a.explanation}
            </li>
          ))}
        </ul>
        <p>
          Потерянные подтверждённые функции:{' '}
          {s.lostFunctions.join(', ') || 'не выявлены в выбранных потребностях'}
        </p>
        <ul>
          {s.coverage.map((c) => (
            <li key={c.capabilityId}>
              {catalog?.capabilities.find((f) => f.id === c.capabilityId)
                ?.name ?? c.capabilityId}
              : <Status value={c.state} />
            </li>
          ))}
        </ul>
        {s.items.map((item) => {
          const product = catalog?.products.find(
            (p) => p.id === item.productId,
          );
          return (
            <p key={item.id}>
              {product ? (
                <Link
                  to={`/catalog/${product.slug}?version=${report.catalogVersion}`}
                >
                  {product.name}: тариф, агенты и источники
                </Link>
              ) : (
                itemName(item.id)
              )}
              {item.includedAgentIds.length > 0 &&
                ` · включённые агенты: ${item.includedAgentIds.join(', ')} (без второй покупки)`}
            </p>
          );
        })}
        <ul>
          {s.costs.unknowns.map((x) => (
            <li key={x}>{readable(x)}</li>
          ))}
          {s.conditions.map((x) => (
            <li key={x}>{readable(x)}</li>
          ))}
        </ul>
        <details>
          <summary>График будущих списаний</summary>
          <ul>
            {s.costs.charges.map((c, i) => (
              <li key={i}>
                {itemName(c.subscriptionId)}: {c.date} ({c.billingZone}) —{' '}
                {money(c.amount, report.currency)}
                {c.unavoidable ? ' · обязательный по указанным условиям' : ''}
              </li>
            ))}
          </ul>
        </details>
      </article>
    );
  }
  return (
    <section aria-label="Сравнение наборов">
      <h2>{decisions[report.decision] ?? report.decision}</h2>
      <p>
        Расчёт {report.asOf}, горизонт {report.horizonMonths} мес. Каталог{' '}
        {report.catalogVersion}.{' '}
        {report.recommendations.datasetKind === 'DEMO' &&
          'DEMO — синтетические тестовые данные.'}
      </p>
      <div className={styles.grid}>
        {scenario(report.current)}
        {report.alternatives.map(scenario)}
      </div>
      {!report.alternatives.length && (
        <p>
          Подтверждённых допустимых альтернатив нет. Это не означает, что их нет
          на рынке.
        </p>
      )}
      <h3>Пересечения функций</h3>
      <ul>
        {report.overlaps.map((o) => (
          <li key={`${o.leftId}:${o.rightId}`}>
            {names[o.leftId]} / {names[o.rightId]}:{' '}
            {overlapLabels[o.status] ?? o.status}. {o.capabilities.join(', ')}.{' '}
            {o.explanation}
          </li>
        ))}
      </ul>
      <h3>Ограничения расчёта</h3>
      <ul>
        {report.limitations.map((x) => (
          <li key={x}>{readable(x)}</li>
        ))}
      </ul>
      <details>
        <summary>Основания, даты и источники</summary>
        <ul>
          {report.recommendations.evidence.map((e) => {
            const source = report.recommendations.sources.find(
              (s) => s.id === e.sourceId,
            );
            return (
              <li key={e.id}>
                {e.summary} · <Status value={e.effectiveStatus} /> ·{' '}
                {e.verifiedAt ?? 'UNKNOWN'} · {e.conditions}
                {source && /^https?:\/\//.test(source.url) && (
                  <>
                    {' '}
                    ·{' '}
                    <a href={source.url} target="_blank" rel="noreferrer">
                      {source.publisher}
                    </a>
                  </>
                )}
              </li>
            );
          })}
        </ul>
      </details>
    </section>
  );
}
