import { useEffect, useState } from 'react';
import { readHealth } from '../api/health';
import styles from './App.module.css';

type ConnectionState = 'loading' | 'ready' | 'error';

export function App() {
  const [attempt, setAttempt] = useState(0);
  const [connection, setConnection] = useState<ConnectionState>('loading');

  useEffect(() => {
    const controller = new AbortController();
    readHealth(controller.signal)
      .then(() => {
        if (!controller.signal.aborted) setConnection('ready');
      })
      .catch(() => {
        if (!controller.signal.aborted) setConnection('error');
      });
    return () => controller.abort();
  }, [attempt]);

  function retry() {
    setConnection('loading');
    setAttempt((value) => value + 1);
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <a href="/" className={styles.brand} aria-label="AI Compass — главная">
          <span aria-hidden="true" className={styles.mark}>
            ↗
          </span>{' '}
          AI Compass
        </a>
        <span className={styles.preview}>Локальная версия</span>
      </header>
      <main className={styles.main}>
        <p className={styles.eyebrow}>YOUR PERSONAL AI STACK</p>
        <h1>
          Ваши потребности.
          <br />
          Ваш бюджет.
          <br />
          <em>Ваш набор AI.</em>
        </h1>
        <p className={styles.intro}>
          Подбор сочетаний AI-сервисов, подписок и агентов для нескольких
          потребностей — с учётом бюджета и ограничений. Без ввода конкретного
          задания.
        </p>
        <section className={styles.card} aria-labelledby="stage-title">
          <span className={styles.stage}>01 / ОСНОВА</span>
          <h2 id="stage-title">Готовим основу сервиса</h2>
          <p>
            Это начальная версия приложения. Каталог и подбор наборов появятся
            на следующих этапах.
          </p>
          <div className={styles.connection} role="status" aria-live="polite">
            <span
              aria-hidden="true"
              className={`${styles.dot} ${styles[connection]}`}
            />
            {connection === 'loading' && 'Проверяем подключение…'}
            {connection === 'ready' && 'Сервис доступен'}
            {connection === 'error' && 'Не удалось подключиться к сервису'}
          </div>
          <button onClick={retry} disabled={connection === 'loading'}>
            Проверить подключение <span aria-hidden="true">↻</span>
          </button>
        </section>
      </main>
      <footer className={styles.footer}>
        AI Compass <span>Осознанный выбор инструментов</span>
      </footer>
    </div>
  );
}
