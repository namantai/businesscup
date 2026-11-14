import { Link } from 'react-router';
import layout from './Monitoring.module.css';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../account/AuthSession';
import { PersonalGate, PersonalError } from '../account/PersonalGate';
import { CatalogLayout } from '../catalog/CatalogLayout';
import {
  checked,
  protection,
  watches,
  notices,
  watch,
  preferences,
  acknowledge,
  savedStacks,
} from '../api/account';
import type { Notice, RecommendationResult } from '../api/generated/types.gen';
import styles from '../builder/Builder.module.css';
import { readBuilderCatalog } from '../api/builder';

export function InboxPage() {
  const { account } = useAuth();
  return (
    <CatalogLayout>
      <div className={layout.workspace}>
        <h1>Изменения моего набора</h1>
        <PersonalGate>
          {account && <Inbox key={account.id} owner={account.id} />}
        </PersonalGate>
      </div>
    </CatalogLayout>
  );
}
function Inbox({ owner }: { owner: string }) {
  const { account } = useAuth();
  const premium = account?.plan === 'PREMIUM';
  const cache = useQueryClient();
  const [busy, setBusy] = useState(false),
    [email, setEmail] = useState(false),
    [consent, setConsent] = useState(false);
  const [confirm, setConfirm] = useState(false),
    [error, setError] = useState<unknown>(null);
  const query = useQuery({
    queryKey: ['personal', owner, 'monitoring'],
    queryFn: async () => {
      const headers = { 'X-Compass-Account': owner };
      const [subscriptions, messages, stacks] = await Promise.all([
        checked(watches({ headers })),
        checked(notices({ headers })),
        checked(savedStacks({ headers })),
      ]);
      return { subscriptions, messages, stacks };
    },
  });
  async function mutate(
    action: (guard: Awaited<ReturnType<typeof protection>>) => Promise<unknown>,
  ) {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      await action(await protection(owner));
      await cache.invalidateQueries({
        queryKey: ['personal', owner, 'monitoring'],
      });
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <p>
        Мониторинг сравнивает опубликованные условия. Небольшие изменения цены
        при известном пороге и оформление источника не создают рекомендацию.
        Внешняя отправка писем отключена.
      </p>
      {query.isPending && <p role="status">Загружаем уведомления…</p>}
      {query.error && <PersonalError error={query.error} />}
      {error && <PersonalError error={error} />}
      <button disabled={busy} onClick={() => void query.refetch()}>
        Обновить уведомления
      </button>
      {!premium && (
        <p>
          Новые уведомления доступны с действующим Premium beta.{' '}
          <Link to="/account/billing">Проверить доступ</Link>. Существующие
          сообщения и отписка доступны всегда.
        </p>
      )}
      <fieldset disabled={busy || !premium}>
        <legend>Следить за сохранённым набором</legend>
        <label>
          <input
            type="checkbox"
            checked={consent}
            onChange={(e) => setConsent(e.target.checked)}
          />
          Согласен на повторный расчёт сохранённых потребностей
        </label>
        <label>
          <input
            type="checkbox"
            checked={email}
            onChange={(e) => setEmail(e.target.checked)}
          />
          Сохранять копию в локальный email outbox (без отправки)
        </label>
        {!query.data?.stacks.length && (
          <p>
            Сначала сохраните результат и выбранный набор в «Мои сохранения».
          </p>
        )}
        <ul>
          {query.data?.stacks.map((s) => (
            <li key={s.id}>
              Набор от{' '}
              {new Date(s.createdAt).toLocaleDateString('ru-RU', {
                timeZone: 'UTC',
              })}{' '}
              <button
                disabled={
                  !consent ||
                  query.data.subscriptions.some((w) => w.stackId === s.id)
                }
                onClick={() =>
                  void mutate((g) =>
                    checked(
                      watch({
                        ...g,
                        body: { stackId: s.id, emailOptIn: email, consent },
                      }),
                    ),
                  )
                }
              >
                Следить за набором
              </button>
            </li>
          ))}
        </ul>
      </fieldset>
      <h2>Подписки на изменения</h2>
      <label>
        <input
          type="checkbox"
          checked={confirm}
          onChange={(e) => setConfirm(e.target.checked)}
        />
        Подтверждаю изменение настроек уведомлений
      </label>
      <ul>
        {query.data?.subscriptions.map((w) => (
          <li key={w.id}>
            {w.effectiveActive
              ? 'Мониторинг включён'
              : w.active
                ? 'Мониторинг приостановлен: beta-доступ не действует'
                : 'Мониторинг выключен'}
            ; email outbox: {w.emailOptIn ? 'включён' : 'выключен'}.
            <button
              disabled={busy || !confirm}
              onClick={() =>
                void mutate((g) =>
                  checked(
                    preferences({
                      ...g,
                      path: { id: w.id },
                      body: { active: !w.active, emailOptIn: false, confirm },
                    }),
                  ),
                )
              }
            >
              {w.active ? 'Отписаться' : 'Включить мониторинг'}
            </button>
            {w.emailOptIn && (
              <button
                disabled={busy || !confirm}
                onClick={() =>
                  void mutate((g) =>
                    checked(
                      preferences({
                        ...g,
                        path: { id: w.id },
                        body: { active: w.active, emailOptIn: false, confirm },
                      }),
                    ),
                  )
                }
              >
                Отключить email outbox
              </button>
            )}
          </li>
        ))}
      </ul>
      <h2>Входящие</h2>
      {query.data?.messages.length === 0 && (
        <p>Существенных изменений пока нет.</p>
      )}
      {query.data?.messages.map((n) => (
        <NoticeCard
          key={n.id}
          notice={n}
          busy={busy}
          onRead={() =>
            void mutate((g) =>
              checked(
                acknowledge({
                  ...g,
                  path: { id: n.id },
                  body: { appliedSelfReported: false },
                }),
              ),
            )
          }
          onApply={() =>
            void mutate((g) =>
              checked(
                acknowledge({
                  ...g,
                  path: { id: n.id },
                  body: { appliedSelfReported: true },
                }),
              ),
            )
          }
        />
      ))}
    </>
  );
}
export function NoticeCard({
  notice: n,
  busy,
  onApply,
  onRead,
}: {
  notice: Notice;
  busy: boolean;
  onApply: () => void;
  onRead: () => void;
}) {
  return (
    <article className={styles.card}>
      <h3>
        Условия изменились ·{' '}
        {new Date(n.createdAt).toLocaleDateString('ru-RU', { timeZone: 'UTC' })}
      </h3>
      <p>{n.comparison.explanation}</p>
      <p>
        Публикация {n.publicationRevision}.{' '}
        {n.appliedSelfReported
          ? 'Применено со слов пользователя; покупка и экономия не подтверждены.'
          : 'Действия у провайдера выполняете вы.'}
      </p>
      <button disabled={busy || n.read} onClick={onRead}>
        {n.read ? 'Прочитано' : 'Отметить прочитанным'}
      </button>
      <h4>Было → стало</h4>
      <ul>
        {n.comparison.changes.map((c) => (
          <li key={c.field}>
            <strong>
              {{
                amount: 'Цена',
                upfrontAmount: 'Предоплата',
                additionalTax: 'Дополнительный налог',
                additionalFee: 'Комиссия',
                state: 'Доступность',
                status: 'Достоверность',
                conditions: 'Условия',
                quantity: 'Лимит',
              }[c.field.split('/').at(-1) ?? ''] ?? 'Условие каталога'}
            </strong>
            : {c.before} → {c.after}
          </li>
        ))}
      </ul>
      <CostSummary
        title="До изменения"
        result={n.comparison.before}
        months={n.horizonMonths}
      />
      <CostSummary
        title="После изменения"
        result={n.comparison.after}
        months={n.horizonMonths}
      />
      <details>
        <summary>Источники и достоверность</summary>
        <ul>
          {n.comparison.after.evidence.map((e) => (
            <li key={e.id}>
              {e.summary} — {e.effectiveStatus}, {e.verifiedAt ?? 'UNKNOWN'}.{' '}
              {e.conditions}
            </li>
          ))}
        </ul>
        <ul>
          {n.comparison.after.sources
            .filter((s) => /^https?:\/\//.test(s.url))
            .map((s) => (
              <li key={s.id}>
                <a href={s.url} target="_blank" rel="noreferrer">
                  {s.publisher}
                </a>
              </li>
            ))}
        </ul>
      </details>
      <button disabled={busy || n.appliedSelfReported} onClick={onApply}>
        Отметить применение (со слов пользователя)
      </button>
    </article>
  );
}
function CostSummary({
  title,
  result,
  months,
}: {
  title: string;
  result: RecommendationResult;
  months: number;
}) {
  const names = useQuery({
    queryKey: ['notice-names', result.catalogVersion],
    enabled: !!result.catalogVersion,
    queryFn: ({ signal }) => readBuilderCatalog(result.catalogVersion!, signal),
  });
  return (
    <section>
      <h4>{title}</h4>
      <p>
        {result.status} ·{' '}
        {result.datasetKind === 'DEMO'
          ? 'DEMO: синтетические данные'
          : 'Опубликованный каталог'}
      </p>
      {!result.bundles.length && (
        <p>Подтверждённый набор отсутствует. Возможны UNKNOWN и ограничения.</p>
      )}
      {result.bundles.map((b) => (
        <div key={b.id}>
          <p>
            {b.items.map((i, index) => {
              const product = names.data?.products.find(
                (p) => p.id === i.productId,
              );
              const plan = names.data?.plans.find((p) => p.id === i.planId);
              return (
                <span key={i.id}>
                  {index > 0 && ', '}
                  {product ? (
                    <Link
                      to={`/catalog/${product.slug}?version=${result.catalogVersion}`}
                    >
                      {product.name}
                      {plan ? ' · ' + plan.tier : ''}
                    </Link>
                  ) : (
                    'Название тарифа пока недоступно'
                  )}
                </span>
              );
            })}
          </p>
          <p>
            Сейчас: {b.costs.cashDueNow ?? 'UNKNOWN'} {b.costs.currency}; в
            месяц: {b.costs.recurringMonthly ?? 'UNKNOWN'}; за {months} мес.:{' '}
            {b.costs.horizonTotal ?? 'UNKNOWN'}. Compass:{' '}
            {b.costs.compassFeeMonthly ?? 'UNKNOWN'} в месяц.
          </p>
          <ul>
            {b.portfolioActions.map((a) => (
              <li key={a.subscriptionId}>
                {a.action}: {a.explanation}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}
