import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { CatalogLayout } from '../catalog/CatalogLayout';
import { checked, protection, export_, deleteAccount } from '../api/account';
import { useAuth } from './AuthSession';
import { PersonalGate, PersonalError } from './PersonalGate';
import styles from '../builder/Builder.module.css';
export function AccountPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const lock = useRef(false);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  async function run(action: () => Promise<void>) {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e);
    } finally {
      lock.current = false;
      setBusy(false);
      setPassword('');
    }
  }
  return (
    <CatalogLayout>
      <h1 ref={heading} tabIndex={-1}>
        Настройки аккаунта
      </h1>
      <PersonalGate>
        {auth.account && (
          <section className={styles.panel}>
            <h2>{auth.account.email}</h2>
            <p>Email подтверждён · Тариф Free · Роль {auth.account.role}</p>
            <p>
              Один сохранённый профиль. Платные подписки Compass и рассылки не
              подключены.
            </p>
            <Link to="/dashboard">Мои сохранения</Link>
            <div className={styles.actions}>
              <button
                className={styles.secondary}
                disabled={busy}
                onClick={() =>
                  void run(async () => {
                    await auth.signOut();
                    navigate('/');
                  })
                }
              >
                Выйти
              </button>
              <Link to="/forgot-password">
                Сменить пароль через подтверждение email
              </Link>
              <button
                className={styles.secondary}
                disabled={busy}
                onClick={() =>
                  void run(async () => {
                    const data = await checked(
                      export_({
                        headers: { 'X-Compass-Account': auth.account!.id },
                      }),
                    );
                    const url = URL.createObjectURL(
                      new Blob([JSON.stringify(data, null, 2)], {
                        type: 'application/json',
                      }),
                    );
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'ai-compass-data.json';
                    a.click();
                    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
                  })
                }
              >
                Экспортировать мои данные
              </button>
            </div>
          </section>
        )}
        <form
          className={styles.panel}
          onSubmit={(e) => {
            e.preventDefault();
            if (!confirm) return;
            void run(async () => {
              await checked(
                deleteAccount({
                  ...(await protection(auth.account?.id)),
                  body: { password, confirm },
                }),
              );
              auth.accept(null);
              navigate('/');
            });
          }}
        >
          <h2>Удалить аккаунт</h2>
          <p>
            Профиль, сохранённые результаты и выбранные наборы будут удалены.
            Все сессии прекратят действовать. Общий каталог сохранится.
          </p>
          <label className={styles.field}>
            Текущий пароль
            <input
              type="password"
              autoComplete="current-password"
              required
              maxLength={128}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <label className={styles.check}>
            <input
              type="checkbox"
              checked={confirm}
              onChange={(e) => setConfirm(e.target.checked)}
            />
            Подтверждаю удаление аккаунта и личных данных
          </label>
          <button
            className={styles.secondary}
            disabled={busy || !confirm}
            type="submit"
          >
            Удалить аккаунт безвозвратно
          </button>
        </form>
        {error !== null && <PersonalError error={error} />}
      </PersonalGate>
    </CatalogLayout>
  );
}
