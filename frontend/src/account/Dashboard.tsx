import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { CatalogLayout } from '../catalog/CatalogLayout';
import {
  checked,
  profiles,
  savedRecommendations,
  savedStacks,
  deleteProfile,
  deleteRecommendation,
  deleteStack,
  protection,
} from '../api/account';
import { readBuilderCatalog } from '../api/builder';
import { useAuth } from './AuthSession';
import { PersonalGate, PersonalError } from './PersonalGate';
import { useBuilder } from '../builder/BuilderSession';
import type { SavedRecommendation } from '../api/generated/types.gen';
import styles from '../builder/Builder.module.css';
export function Dashboard() {
  const { account } = useAuth();
  const builder = useBuilder();
  const navigate = useNavigate();
  const heading = useRef<HTMLHeadingElement>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const request = useRef<AbortController | null>(null);
  const data = useQuery({
    queryKey: ['personal', account?.id, 'dashboard'],
    enabled: !!account,
    queryFn: async ({ signal }) => {
      const [p, r, s] = await Promise.all([
        checked(
          profiles({ signal, headers: { 'X-Compass-Account': account!.id } }),
        ),
        checked(
          savedRecommendations({
            signal,
            headers: { 'X-Compass-Account': account!.id },
          }),
        ),
        checked(
          savedStacks({
            signal,
            headers: { 'X-Compass-Account': account!.id },
          }),
        ),
      ]);
      return { profiles: p, recommendations: r, stacks: s };
    },
  });
  useEffect(() => {
    heading.current?.focus();
    return () => request.current?.abort();
  }, []);
  useEffect(() => () => request.current?.abort(), [account?.id]);
  async function open(saved: SavedRecommendation) {
    if (!saved.result.catalogVersion || busy) return;
    const controller = new AbortController();
    request.current = controller;
    setBusy(true);
    setError(null);
    try {
      const catalog = await readBuilderCatalog(
        saved.result.catalogVersion,
        controller.signal,
      );
      if (!controller.signal.aborted) {
        builder.restore({
          profile: saved.profile,
          response: saved.result,
          catalog,
        });
        navigate('/results');
      }
    } catch (e) {
      if (!controller.signal.aborted) setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function remove(kind: 'profile' | 'result' | 'stack', id: string) {
    if (busy) return;
    setBusy(true);
    setError(null);
    try {
      const opts = { ...(await protection(account?.id)), path: { id } };
      await checked(
        kind === 'profile'
          ? deleteProfile(opts)
          : kind === 'result'
            ? deleteRecommendation(opts)
            : deleteStack(opts),
      );
      await data.refetch();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <CatalogLayout>
      <h1 ref={heading} tabIndex={-1}>
        Мои AI-профили и результаты
      </h1>
      <PersonalGate>
        <p>
          <Link to="/account">Настройки аккаунта</Link> ·{' '}
          <Link to="/onboarding">Новый подбор</Link>
        </p>
        {data.isPending && <p role="status">Загружаем сохранения…</p>}
        {data.isError && (
          <>
            <PersonalError error={data.error} />
            <button onClick={() => void data.refetch()}>Повторить</button>
          </>
        )}
        {data.data && (
          <>
            <section className={styles.panel}>
              <h2>Сохранённый профиль</h2>
              {!data.data.profiles.length && (
                <p>
                  Пока нет профиля. Сохраните его после подбора; Free позволяет
                  один профиль.
                </p>
              )}
              {data.data.profiles.map((p) => (
                <article key={p.id}>
                  <h3>{p.name}</h3>
                  <p>
                    Версия {p.revision} · {p.profile.country} ·{' '}
                    {p.profile.budget.monthlyMax} {p.profile.budget.currency} /
                    месяц
                  </p>
                  <p>
                    Перед новым подбором проверьте даты продления текущих
                    подписок.
                  </p>
                  <div className={styles.actions}>
                    <button
                      className={styles.primary}
                      onClick={() => {
                        builder.load(p.profile);
                        navigate('/onboarding');
                      }}
                    >
                      Открыть профиль и повторить подбор
                    </button>
                    <button
                      className={styles.secondary}
                      disabled={busy}
                      onClick={() => void remove('profile', p.id)}
                    >
                      Удалить профиль
                    </button>
                  </div>
                </article>
              ))}
            </section>
            <h2>Исторические результаты</h2>
            {!data.data.recommendations.length && (
              <p>Сохранённых результатов пока нет.</p>
            )}
            {data.data.recommendations.map((r) => (
              <article key={r.id} className={styles.card}>
                <h3>
                  Расчёт от {new Date(r.result.asOf).toLocaleString('ru')}
                </h3>
                <p>
                  Страна: {r.profile.country} · {r.profile.budget.monthlyMax}{' '}
                  {r.profile.budget.currency} / месяц
                </p>
                <p>
                  Каталог: {r.result.catalogVersion} · движок{' '}
                  {r.result.engineVersion}. Это исторический результат, он не
                  обновляется автоматически.
                </p>
                <div className={styles.actions}>
                  <button
                    className={styles.primary}
                    disabled={busy}
                    onClick={() => void open(r)}
                  >
                    Открыть сохранённый результат
                  </button>
                  <button
                    className={styles.secondary}
                    disabled={busy}
                    onClick={() => void remove('result', r.id)}
                  >
                    Удалить результат
                  </button>
                </div>
              </article>
            ))}
            <h2>Выбранные наборы</h2>
            {!data.data.stacks.length && (
              <p>Вы пока не выбрали сохранённый набор.</p>
            )}
            {data.data.stacks.map((s) => (
              <article className={styles.card} key={s.id}>
                <p>{s.bundleId}</p>
                <button
                  className={styles.secondary}
                  disabled={busy}
                  onClick={() => void remove('stack', s.id)}
                >
                  Удалить выбранный набор
                </button>
              </article>
            ))}
          </>
        )}
        {error !== null && <PersonalError error={error} />}
      </PersonalGate>
    </CatalogLayout>
  );
}
