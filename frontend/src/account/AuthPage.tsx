import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router';
import { CatalogLayout } from '../catalog/CatalogLayout';
import {
  checked,
  protection,
  login,
  register,
  verify,
  forgot,
  resend,
  reset,
  accountError,
} from '../api/account';
import { useAuth } from './AuthSession';
import { useBuilder } from '../builder/BuilderSession';
import styles from '../builder/Builder.module.css';
export type AuthMode = 'login' | 'register' | 'verify' | 'forgot' | 'reset';
const titles: Record<AuthMode, string> = {
  login: 'Войти',
  register: 'Создать аккаунт',
  verify: 'Подтвердить email',
  forgot: 'Восстановить пароль',
  reset: 'Новый пароль',
};
export function AuthPage({ mode }: { mode: AuthMode }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [token, setToken] = useState(
    () => new URLSearchParams(location.hash.slice(1)).get('token') ?? '',
  );
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState<unknown>(null);
  const lock = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);
  const alert = useRef<HTMLDivElement>(null);
  const auth = useAuth();
  const builder = useBuilder();
  const navigate = useNavigate();
  useEffect(() => {
    if (location.hash)
      history.replaceState(
        history.state,
        '',
        location.pathname + location.search,
      );
    heading.current?.focus();
  }, []);
  useEffect(() => {
    if (error) alert.current?.focus();
  }, [error]);
  async function run(action: () => Promise<void>) {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError(null);
    setMessage('');
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
      <h1 tabIndex={-1} ref={heading}>
        {titles[mode]}
      </h1>
      <p>
        Гостевой подбор остаётся доступным. Регистрация сама по себе не
        сохраняет анкету и результат.
      </p>
      {builder.completed && (
        <p className={styles.notice}>
          Ваш результат остаётся в памяти этой вкладки. Для сохранения после
          входа потребуется отдельное подтверждение. Не перезагружайте эту
          вкладку.
        </p>
      )}
      <form
        className={styles.panel}
        onSubmit={(e) => {
          e.preventDefault();
          void run(async () => {
            const guard = await protection();
            if (mode === 'login') {
              const account = await checked(
                login({ ...guard, body: { email, password } }),
              );
              auth.accept(account);
              navigate(builder.completed ? '/results' : '/dashboard');
            } else if (mode === 'register') {
              await checked(register({ ...guard, body: { email, password } }));
              setMessage(
                'Если адрес подходит, письмо доступно в локальном почтовом ящике. Подтвердите email, затем войдите.',
              );
            } else if (mode === 'verify') {
              await checked(verify({ ...guard, body: { token } }));
              setToken('');
              setMessage('Email подтверждён. Теперь можно войти.');
            } else if (mode === 'forgot') {
              await checked(forgot({ ...guard, body: { email } }));
              setMessage(
                'Если адрес подходит, инструкция доступна в локальном почтовом ящике.',
              );
            } else {
              await checked(reset({ ...guard, body: { token, password } }));
              auth.accept(null);
              setToken('');
              setMessage('Пароль изменён. Войдите заново на всех устройствах.');
            }
          });
        }}
      >
        <fieldset
          disabled={busy}
          style={{ border: 0, padding: 0, minWidth: 0 }}
        >
          {(mode === 'login' || mode === 'register' || mode === 'forgot') && (
            <label className={styles.field}>
              Email
              <input
                type="email"
                autoComplete="email"
                required
                maxLength={254}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>
          )}
          {(mode === 'verify' || mode === 'reset') && (
            <label className={styles.field}>
              Код из письма
              <input
                type="password"
                autoComplete="off"
                required
                value={token}
                onChange={(e) => setToken(e.target.value.trim())}
              />
            </label>
          )}
          {(mode === 'login' || mode === 'register' || mode === 'reset') && (
            <label className={styles.field}>
              Пароль
              <input
                type="password"
                autoComplete={
                  mode === 'login' ? 'current-password' : 'new-password'
                }
                minLength={12}
                maxLength={128}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <span>От 12 до 128 символов.</span>
            </label>
          )}
          <button className={styles.primary} type="submit" disabled={busy}>
            {busy ? 'Выполняем…' : titles[mode]}
          </button>
        </fieldset>
        {error !== null && (
          <div className={styles.error} role="alert" tabIndex={-1} ref={alert}>
            {accountError(error)}
          </div>
        )}
        {message && (
          <p role="status" className={styles.notice}>
            {message}
          </p>
        )}
      </form>
      <div className={styles.actions}>
        <Link to="/login">Войти в аккаунт</Link>
        <Link to="/register">Регистрация</Link>
        <Link to="/verify-email">Подтверждение email</Link>
        <Link to="/forgot-password">Забыли пароль?</Link>
        <Link to="/onboarding">Продолжить без аккаунта</Link>
      </div>
      {(mode === 'register' || mode === 'forgot') && (
        <p>
          После подтверждения email вернитесь в эту вкладку: здесь остаётся ваш
          гостевой результат.
        </p>
      )}
      {(mode === 'login' || mode === 'register') && (
        <button
          className={styles.secondary}
          disabled={busy || !email}
          onClick={() =>
            void run(async () => {
              await checked(
                resend({ ...(await protection()), body: { email } }),
              );
              setMessage(
                'Если адрес подходит, новое письмо доступно в локальном почтовом ящике.',
              );
            })
          }
        >
          Отправить подтверждение ещё раз
        </button>
      )}
    </CatalogLayout>
  );
}
