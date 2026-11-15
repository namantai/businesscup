import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { Navigation } from '../app/Navigation';
import styles from './Catalog.module.css';

export function CatalogLayout({ children }: { children: ReactNode }) {
  return (
    <div className={styles.page}>
      <a className={styles.skip} href="#catalog-main">
        Перейти к содержимому
      </a>
      <header className={styles.header}>
        <Link className={styles.brand} to="/">
          <span className={styles.mark} aria-hidden="true">
            ↗
          </span>{' '}
          AI Compass
        </Link>
        <Navigation />
      </header>
      <main id="catalog-main" className={styles.main}>
        {children}
      </main>
      <footer className={styles.footer}>
        AI Compass · Осознанный выбор. Без промпта.
      </footer>
    </div>
  );
}
export function Loading() {
  return <p role="status">Загружаем каталог…</p>;
}
export function ErrorState({
  retry,
  error,
}: {
  retry: () => void;
  error?: unknown;
}) {
  const missing =
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    error.status === 404;
  return (
    <section role="alert">
      <h2>
        {missing
          ? 'Запись или опубликованная версия не найдена'
          : 'Не удалось загрузить каталог'}
      </h2>
      <p>
        Проверьте подключение и повторите запрос. Данные не заменены
        демонстрационными.
      </p>
      <button onClick={retry}>Повторить</button>
    </section>
  );
}
export function Status({ value }: { value: string }) {
  const labels: Record<string, string> = {
    UNKNOWN: 'Неизвестно',
    ELIGIBLE: 'Соответствует условиям',
    INELIGIBLE: 'Не соответствует условиям',
    VERIFIED_SOURCE: 'Подтверждено источником',
    CONDITIONAL: 'При условиях',
    STALE: 'Устарело',
    CONFLICTED: 'Источники расходятся',
    DEMO: 'Синтетические данные',
    SUPPORTED: 'Поддерживается',
    PARTIAL: 'Частично',
    UNSUPPORTED: 'Не поддерживается',
    AVAILABLE: 'Доступно',
    REQUIRED: 'Требуется',
    NOT_REQUIRED: 'Не требуется',
    KNOWN: 'Известно',
    UNLIMITED: 'Без указанного лимита',
  };
  return (
    <span className={styles.badge} data-status={value}>
      {labels[value] ?? value}
    </span>
  );
}
export function DateLabel({ value }: { value: string | null }) {
  if (!value) return <>Дата проверки неизвестна</>;
  return (
    <time dateTime={value}>
      {new Intl.DateTimeFormat('ru-RU', {
        dateStyle: 'medium',
        timeZone: 'UTC',
      }).format(new Date(value))}{' '}
      (UTC)
    </time>
  );
}
