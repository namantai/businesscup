import { useRef, useState } from 'react';
import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from './AuthSession';
import { useBuilder } from '../builder/BuilderSession';
import {
  checked,
  profiles,
  createProfile,
  updateProfile,
  saveRecommendation,
  saveStack,
  protection,
} from '../api/account';
import { PersonalError } from './PersonalGate';
import styles from '../builder/Builder.module.css';
export function SavePanel() {
  const { account } = useAuth();
  const { completed } = useBuilder();
  const list = useQuery({
    queryKey: ['personal', account?.id, 'profiles'],
    queryFn: ({ signal }) =>
      checked(
        profiles({ signal, headers: { 'X-Compass-Account': account!.id } }),
      ),
    enabled: !!account,
  });
  const [consent, setConsent] = useState(false);
  const [name, setName] = useState('Мой AI-профиль');
  const [selected, setSelected] = useState('');
  const [profileTarget, setProfileTarget] = useState('');
  const existingProfile =
    profileTarget === 'NEW'
      ? undefined
      : (list.data?.find((p) => p.id === profileTarget) ?? list.data?.[0]);
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  const [error, setError] = useState<unknown>(null);
  const [message, setMessage] = useState('');
  if (!completed) return null;
  if (!account)
    return (
      <section className={styles.panel}>
        <h2>Вернуться к результату позже</h2>
        <p>
          Для сохранения войдите или зарегистрируйтесь. После входа подтвердите
          сохранение отдельно.
        </p>
        <Link to="/login">Сохранить результат — войти</Link> ·{' '}
        <Link to="/register">Создать аккаунт</Link>
      </section>
    );
  async function save(kind: 'profile' | 'result') {
    if (!completed || !account || !consent || lock.current) return;
    lock.current = true;
    setBusy(true);
    setError(null);
    setMessage('');
    try {
      const guard = await protection(account?.id);
      if (kind === 'profile') {
        const existing = existingProfile;
        const body = {
          name,
          profile: { ...completed.profile, asOf: null },
          revision: existing?.revision ?? 0,
        };
        if (existing)
          await checked(
            updateProfile({ ...guard, path: { id: existing.id }, body }),
          );
        else await checked(createProfile({ ...guard, body }));
        await list.refetch();
        setMessage('Профиль сохранён в аккаунте.');
      } else {
        if (!completed.response.catalogVersion) throw new Error('No catalog');
        const saved = await checked(
          saveRecommendation({
            ...guard,
            body: {
              profile: completed.profile,
              catalogVersion: completed.response.catalogVersion,
              asOf: completed.response.asOf,
              engineVersion: completed.response.engineVersion,
              policyVersion: completed.response.policyVersion,
            },
          }),
        );
        if (selected)
          await checked(
            saveStack({
              ...(await protection(account?.id)),
              body: { snapshotId: saved.id, bundleId: selected },
            }),
          );
        setMessage(
          'Исторический результат сохранён' +
            (selected ? ' вместе с выбранным набором.' : '.'),
        );
      }
    } catch (e) {
      setError(e);
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return (
    <section className={styles.panel}>
      <h2>Сохранить в аккаунт</h2>
      <p>
        Аккаунт: {account.email}. Free — один профиль. Старые результаты
        сохраняют версию каталога, дату и неизвестные данные.
      </p>
      {!!list.data?.length && (
        <label className={styles.field}>
          Как сохранить профиль
          <select
            value={profileTarget || list.data[0].id}
            onChange={(e) => setProfileTarget(e.target.value)}
          >
            {list.data.map((p) => (
              <option key={p.id} value={p.id}>
                Обновить: {p.name}
              </option>
            ))}
            {account.plan === 'PREMIUM' && list.data.length < 5 && (
              <option value="NEW">Создать отдельный профиль</option>
            )}
          </select>
        </label>
      )}
      <label className={styles.field}>
        Название профиля
        <input
          maxLength={100}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </label>
      <label className={styles.field}>
        Выбранный AI-набор
        <select value={selected} onChange={(e) => setSelected(e.target.value)}>
          <option value="">Сохранить результат без выбора набора</option>
          {[
            ...completed.response.bundles,
            ...completed.response.conditionalAlternatives,
          ].map((b, i) => (
            <option key={b.id} value={b.id}>
              Набор {i + 1}: {b.strategies.join(' / ') || 'условный вариант'}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.check}>
        <input
          type="checkbox"
          checked={consent}
          onChange={(e) => setConsent(e.target.checked)}
        />
        Подтверждаю сохранение указанных параметров в этот аккаунт
      </label>
      <div className={styles.actions}>
        <button
          className={styles.primary}
          disabled={
            busy || !consent || !name.trim() || list.isPending || list.isError
          }
          onClick={() => void save('profile')}
        >
          {existingProfile
            ? 'Обновить сохранённый профиль'
            : 'Сохранить профиль'}
        </button>
        <button
          className={styles.secondary}
          disabled={busy || !consent || !completed.response.catalogVersion}
          onClick={() => void save('result')}
        >
          Сохранить результат
        </button>
        <Link to="/dashboard">Мои сохранения</Link>
      </div>
      {busy && <p role="status">Сохраняем…</p>}
      {message && <p role="status">{message}</p>}
      {error !== null && <PersonalError error={error} />}
      {list.isError && <PersonalError error={list.error} />}
    </section>
  );
}
