import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router';
import { useAuth } from './AuthSession';
import { AccountError, accountError } from '../api/account';
export function PersonalGate({ children }: { children: ReactNode }) {
  const auth = useAuth();
  if (auth.loading) return <p role="status">Проверяем сессию…</p>;
  if (auth.failure)
    return (
      <div role="alert">
        {accountError(auth.failure)}{' '}
        <button onClick={() => void auth.refresh()}>Повторить</button>
      </div>
    );
  if (!auth.account)
    return (
      <p role="status">
        Для личных данных нужно войти. <Link to="/login">Войти в аккаунт</Link>{' '}
        или <Link to="/register">зарегистрироваться</Link>. Гостевой подбор
        доступен без входа.
      </p>
    );
  return children;
}
export function PersonalError({ error }: { error: unknown }) {
  const { accept, refresh } = useAuth();
  useEffect(() => {
    if (error instanceof AccountError && error.status === 401) accept(null);
    if (error instanceof AccountError && error.status === 409) void refresh();
  }, [error, accept, refresh]);
  return <p role="alert">{accountError(error)}</p>;
}
