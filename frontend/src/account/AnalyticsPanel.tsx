import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  checked,
  protection,
  analyticsPreferences,
  analyticsConsent,
  analyticsRevisit,
} from '../api/account';
import { PersonalError } from './PersonalGate';
import styles from '../builder/Builder.module.css';

export function AnalyticsPanel({ owner }: { owner: string }) {
  const query = useQuery({
    queryKey: ['personal', owner, 'analytics'],
    queryFn: ({ signal }) =>
      checked(
        analyticsPreferences({
          signal,
          headers: { 'X-Compass-Account': owner },
        }),
      ),
  });
  const [choice, setChoice] = useState<boolean | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState<unknown>(null);
  const visited = useRef(false),
    lock = useRef(false);
  useEffect(() => {
    if (!query.data?.enabled || visited.current) return;
    visited.current = true;
    void protection(owner)
      .then((guard) =>
        checked(analyticsRevisit({ ...guard, body: { confirm: true } })),
      )
      .catch(() => {
        // Optional measurement cannot block account functions. No retry loop or payload logging.
      });
  }, [owner, query.data?.enabled]);
  async function save() {
    if (lock.current || choice === null) return;
    lock.current = true;
    setBusy(true);
    setError(null);
    try {
      await checked(
        analyticsConsent({
          ...(await protection(owner)),
          body: { enabled: choice, confirm: true },
        }),
      );
      await query.refetch();
      setChoice(null);
    } catch (e) {
      setError(e);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return (
    <section className={styles.panel}>
      <h2>Необязательная аналитика продукта</h2>
      <p>
        По умолчанию выключена. Только три факта за день: сохранение результата,
        подтверждение прочтения изменения и посещение аккаунта. Без параметров
        профиля, email, IP, внешних трекеров и платёжных событий. Данные связаны
        с аккаунтом, доступны в экспорте.
      </p>
      <p>
        Окно: 30 дней UTC, очистка не реже суток при работающем приложении.
        Отзыв согласия сразу удаляет события. Это не статистика покупок или
        выручки.
      </p>
      {query.isPending && <p role="status">Загружаем настройку аналитики…</p>}
      {(error || query.error) && <PersonalError error={error || query.error} />}
      <label className={styles.check}>
        <input
          type="checkbox"
          checked={choice ?? query.data?.enabled ?? false}
          disabled={busy || !query.data}
          onChange={(e) => setChoice(e.target.checked)}
        />
        Согласен на локальную аналитику указанных событий
      </label>
      <button
        disabled={busy || choice === null || !query.data}
        onClick={() => void save()}
      >
        Сохранить настройку аналитики
      </button>
      {query.data && (
        <p role="status">
          Сбор {query.data.enabled ? 'включён' : 'выключен'}. Полные события — в
          экспорте аккаунта.
        </p>
      )}
    </section>
  );
}
