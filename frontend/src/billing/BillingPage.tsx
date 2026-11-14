import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { CatalogLayout } from '../catalog/CatalogLayout';
import { useAuth } from '../account/AuthSession';
import { PersonalGate, PersonalError } from '../account/PersonalGate';
import {
  checked,
  protection,
  betaPlans,
  entitlements,
  entitlementAudit,
  inspectEntitlements,
  changeEntitlements,
} from '../api/account';
import type { Entitlements } from '../api/generated/types.gen';
import styles from '../builder/Builder.module.css';

export function PricingPage() {
  const query = useQuery({
    queryKey: ['plans'],
    queryFn: () => checked(betaPlans()),
  });
  return (
    <CatalogLayout>
      <h1>Free и Premium — локальная beta</h1>
      <p>
        Оплата недоступна. Нет автопродления, транзакций и будущих списаний.
        Каталог, источники, ограничения и гостевой подбор доступны бесплатно.
      </p>
      {query.isPending && <p role="status">Загружаем условия…</p>}
      {query.error && (
        <>
          <PersonalError error={query.error} />
          <button onClick={() => void query.refetch()}>Повторить</button>
        </>
      )}
      <div className={styles.grid}>
        {query.data?.map((plan) => (
          <section className={styles.panel} key={plan.name}>
            <h2>{plan.name}</h2>
            <p>{plan.availability}</p>
            <p>
              Сбор Compass сейчас: {plan.feeAmount} {plan.currency}. Списаний
              нет.
            </p>
            <ul>
              <li>Профили: до {plan.profileLimit}</li>
              <li>
                {plan.history
                  ? 'История результатов'
                  : 'Текущий сохранённый результат'}
              </li>
              <li>Отслеживаемые наборы: {plan.watchLimit}</li>
              <li>Ручной учёт подписок и расчёт расходов</li>
              <li>Все источники и неизвестные данные видны</li>
            </ul>
          </section>
        ))}
      </div>
      <p>
        После окончания beta новые расчёты мониторинга приостанавливаются.
        Личные данные можно экспортировать и удалить при любом тарифе.
      </p>
      <Link to="/account/billing">Мой доступ</Link>
    </CatalogLayout>
  );
}
export function BillingPage() {
  const { account } = useAuth();
  return (
    <CatalogLayout>
      <h1>Мой доступ — beta, оплата недоступна</h1>
      <PersonalGate>
        {account && (
          <Access
            key={account.id}
            owner={account.id}
            admin={account.role === 'ADMIN'}
          />
        )}
      </PersonalGate>
    </CatalogLayout>
  );
}
function Access({ owner, admin }: { owner: string; admin: boolean }) {
  const data = useQuery({
    queryKey: ['personal', owner, 'entitlements'],
    queryFn: async () => {
      const headers = { 'X-Compass-Account': owner };
      return {
        rights: await checked(entitlements({ headers })),
        audit: await checked(entitlementAudit({ headers })),
      };
    },
    refetchInterval: 30000,
  });
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  return (
    <>
      <h2 ref={heading} tabIndex={-1}>
        Права аккаунта
      </h2>
      <p>
        Идентификатор для beta-приглашения: <code>{owner}</code>
      </p>
      {data.isPending && <p role="status">Проверяем доступ…</p>}
      {data.error && <PersonalError error={data.error} />}
      <button onClick={() => void data.refetch()}>Обновить доступ</button>
      {data.data && (
        <section className={styles.panel}>
          <Rights value={data.data.rights} />
          <h3>История доступа</h3>
          {!data.data.audit.length && <p>Beta-доступ ещё не выдавался.</p>}
          <ul>
            {data.data.audit.map((event) => (
              <li key={event.requestId}>
                {event.action === 'GRANT'
                  ? 'Выдан временный доступ'
                  : 'Доступ отозван'}{' '}
                · {new Date(event.createdAt).toLocaleString('ru-RU')} · версия{' '}
                {event.revision}
              </li>
            ))}
          </ul>
        </section>
      )}
      <Link to="/pricing">Сравнить возможности</Link>
      <p>
        Банковские данные не собираются. Удаление аккаунта удаляет beta-права и
        их историю; будущих списаний нет.
      </p>
      {admin && <GrantAdmin owner={owner} />}
    </>
  );
}
function Rights({ value }: { value: Entitlements }) {
  return (
    <>
      <h3>{value.plan}</h3>
      <p>
        Состояние beta:{' '}
        {(
          {
            NONE: 'не выдавался',
            ACTIVE: 'действует',
            EXPIRED: 'истёк',
            REVOKED: 'отозван',
          } as Record<string, string>
        )[value.grantState] ?? value.grantState}
      </p>
      <p>
        Профили: {value.profileLimit}; мониторинг: {value.watchLimit}; история:{' '}
        {value.history ? 'доступна' : 'только текущий результат'}.
      </p>
      {value.expiresAt && (
        <p>
          Дата окончания выданного доступа:{' '}
          {new Date(value.expiresAt).toLocaleString('ru-RU')}
        </p>
      )}
      <p>
        Сбор Compass: {value.feeAmount} {value.feeCurrency}. Оплата OFF;
        списаний и автопродления нет.
      </p>
    </>
  );
}
function GrantAdmin({ owner }: { owner: string }) {
  const [target, setTarget] = useState(''),
    [loaded, setLoaded] = useState(''),
    [days, setDays] = useState(7),
    [confirm, setConfirm] = useState(false);
  const [value, setValue] = useState<Entitlements | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState<unknown>(null);
  const lock = useRef(false);
  async function run(action: 'LOAD' | 'GRANT' | 'REVOKE') {
    if (
      lock.current ||
      (action !== 'LOAD' && (loaded !== target || !value || !confirm))
    )
      return;
    lock.current = true;
    setBusy(true);
    setError(null);
    try {
      const response =
        action === 'LOAD'
          ? await checked(
              inspectEntitlements({
                headers: { 'X-Compass-Account': owner },
                path: { owner: target },
              }),
            )
          : await checked(
              changeEntitlements({
                ...(await protection(owner)),
                path: { owner: loaded },
                body: {
                  action,
                  days: action === 'GRANT' ? days : 0,
                  expectedRevision: value!.revision,
                  requestId: crypto.randomUUID(),
                },
              }),
            );
      setValue(response);
      setLoaded(target);
      setConfirm(false);
    } catch (e) {
      setError(e);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return (
    <section className={styles.panel}>
      <h2>ADMIN: временный beta-доступ</h2>
      <p>Выдача и отзыв записываются в журнал. Это приглашение, не платёж.</p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void run('LOAD');
        }}
      >
        <label className={styles.field}>
          UUID аккаунта
          <input
            required
            disabled={busy}
            value={target}
            onChange={(e) => {
              setTarget(e.target.value);
              setValue(null);
              setConfirm(false);
            }}
            pattern="[0-9a-fA-F-]{36}"
            maxLength={36}
          />
        </label>
        <button disabled={busy}>Загрузить права</button>
      </form>
      {error !== null && <PersonalError error={error} />}
      {value && (
        <>
          <Rights value={value} />
          <label className={styles.field}>
            Дней доступа (1–90)
            <input
              type="number"
              min={1}
              max={90}
              value={days}
              onChange={(e) => setDays(Number(e.target.value))}
            />
          </label>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={confirm}
              onChange={(e) => setConfirm(e.target.checked)}
            />
            Подтверждаю изменение beta-доступа
          </label>
          <div className={styles.actions}>
            <button
              disabled={
                busy ||
                !confirm ||
                !Number.isInteger(days) ||
                days < 1 ||
                days > 90
              }
              onClick={() => void run('GRANT')}
            >
              Выдать beta-доступ
            </button>
            <button
              disabled={busy || !confirm || value.revision === 0}
              onClick={() => void run('REVOKE')}
            >
              Отозвать beta-доступ
            </button>
          </div>
        </>
      )}
    </section>
  );
}
