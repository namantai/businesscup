import { useBuilder } from './BuilderSession';
import type { BuilderCatalog } from '../api/builder';
import type { Draft } from './draft';
import styles from './Builder.module.css';
type Subscription = Draft['existingSubscriptions'][number];
export function ExistingSubscriptions({
  catalog,
}: {
  catalog: BuilderCatalog;
}) {
  const { draft, update } = useBuilder();
  function change(id: string, patch: Partial<Subscription>) {
    update((d) => ({
      ...d,
      existingSubscriptions: d.existingSubscriptions.map((s) =>
        s.id === id ? { ...s, ...patch } : s,
      ),
    }));
  }
  return (
    <section aria-labelledby="subscriptions">
      <h3 id="subscriptions" tabIndex={-1}>
        Текущие подписки
      </h3>
      <p className={styles.muted}>
        Укажите фактические платежи. Уже оплаченные периоды учитываются по датам
        продления. Неизвестная цена остаётся неизвестной.
      </p>
      <label className={styles.field}>
        Как обращаться с текущими подписками
        <select
          value={draft.portfolioMode}
          onChange={(e) =>
            update((d) => ({
              ...d,
              portfolioMode: e.target.value as Draft['portfolioMode'],
            }))
          }
        >
          <option value="AUGMENT">Сохранить все, подобрать дополнение</option>
          <option value="REVIEW">Предложить пересмотр к продлению</option>
        </select>
      </label>
      {draft.existingSubscriptions.map((sub, index) => (
        <fieldset key={sub.id} className={styles.card}>
          <legend>Подписка {index + 1}</legend>
          <label className={styles.field}>
            Тариф и предложение
            <select
              id={`sub-${sub.id}-offer`}
              value={sub.offerId ?? ''}
              onChange={(e) => {
                const offer = catalog.offers.find(
                  (o) => o.id === e.target.value,
                );
                change(sub.id, {
                  offerId: offer?.id ?? null,
                  planId: offer?.planId ?? null,
                });
              }}
            >
              <option value="">
                Мой тариф не описан — возможности неизвестны
              </option>
              {catalog.offers.map((o) => {
                const plan = catalog.plans.find((p) => p.id === o.planId);
                const product = catalog.products.find(
                  (p) => p.id === plan?.productId,
                );
                return (
                  <option value={o.id} key={o.id}>
                    {product?.name ?? 'Продукт не описан'} /{' '}
                    {plan?.tier ?? 'Тариф не описан'} · {o.regionId} ·{' '}
                    {o.currency} ·{' '}
                    {o.billingPeriod === 'YEAR'
                      ? 'год'
                      : o.billingPeriod === 'MONTH'
                        ? 'месяц'
                        : o.billingPeriod}
                  </option>
                );
              })}
            </select>
          </label>
          <div className={styles.grid}>
            <label className={styles.field}>
              Валюта платежа
              <input
                id={`sub-${sub.id}-currency`}
                value={sub.currency}
                maxLength={3}
                onChange={(e) =>
                  change(sub.id, { currency: e.target.value.toUpperCase() })
                }
                placeholder="USD"
              />
            </label>
            <label className={styles.field}>
              Фактический платёж
              <input
                id={`sub-${sub.id}-amount`}
                inputMode="decimal"
                value={sub.amount ?? ''}
                disabled={sub.amount === null}
                onChange={(e) => change(sub.id, { amount: e.target.value })}
              />
            </label>
            <label className={styles.field}>
              Период оплаты
              <select
                id={`sub-${sub.id}-period`}
                value={sub.billingPeriod}
                onChange={(e) =>
                  change(sub.id, {
                    billingPeriod: e.target
                      .value as Subscription['billingPeriod'],
                  })
                }
              >
                <option value="">Выберите период</option>
                <option value="MONTH">Каждый месяц</option>
                <option value="YEAR">Каждый год</option>
              </select>
            </label>
          </div>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={sub.amount === null}
              onChange={(e) =>
                change(sub.id, { amount: e.target.checked ? null : '' })
              }
            />
            Цена неизвестна
          </label>
          <div className={styles.grid}>
            <label className={styles.field}>
              Исходная дата продления
              <input
                id={`sub-${sub.id}-anchor`}
                type="date"
                value={sub.renewalAnchor}
                onChange={(e) =>
                  change(sub.id, { renewalAnchor: e.target.value })
                }
              />
            </label>
            <label className={styles.field}>
              Следующее продление
              <input
                type="date"
                value={sub.nextRenewal}
                onChange={(e) =>
                  change(sub.id, { nextRenewal: e.target.value })
                }
              />
            </label>
            <label className={styles.field}>
              Часовой пояс платежа
              <input
                id={`sub-${sub.id}-zone`}
                placeholder="Asia/Almaty"
                value={sub.billingZone}
                onChange={(e) =>
                  change(sub.id, { billingZone: e.target.value })
                }
              />
            </label>
          </div>
          <p className={styles.muted}>
            Исходная дата задаёт день регулярного списания; следующее продление
            должно соответствовать этому календарю и быть не раньше сегодняшнего
            дня.
          </p>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={sub.mustKeep}
              onChange={(e) => change(sub.id, { mustKeep: e.target.checked })}
            />
            Обязательно сохранить (mustKeep)
          </label>
          <button
            type="button"
            className={styles.secondary}
            onClick={() =>
              update((d) => ({
                ...d,
                existingSubscriptions: d.existingSubscriptions.filter(
                  (s) => s.id !== sub.id,
                ),
              }))
            }
          >
            Удалить подписку {index + 1}
          </button>
        </fieldset>
      ))}
      <button
        type="button"
        className={styles.secondary}
        disabled={draft.existingSubscriptions.length >= 8}
        onClick={() =>
          update((d) => ({
            ...d,
            existingSubscriptions: [
              ...d.existingSubscriptions,
              {
                id: crypto.randomUUID(),
                planId: null,
                offerId: null,
                currency: '',
                amount: '',
                billingPeriod: '',
                renewalAnchor: '',
                nextRenewal: '',
                billingZone: '',
                mustKeep: false,
              },
            ],
          }))
        }
      >
        Добавить текущую подписку
      </button>
    </section>
  );
}
