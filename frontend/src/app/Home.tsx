import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { Navigation } from './Navigation';
import builder from '../builder/Builder.module.css';
import { readHealth } from '../api/health';
import styles from './App.module.css';

type ConnectionState = 'loading' | 'ready' | 'error';

export function Home() {
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
        <Link to="/" className={styles.brand} aria-label="AI Compass — главная">
          <span aria-hidden="true" className={styles.mark}>
            ↗
          </span>{' '}
          AI Compass
        </Link>
        <Navigation />
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
          <span className={styles.stage}>ПОТРЕБНОСТИ → УСЛОВИЯ → НАБОР</span>
          <h2 id="stage-title">Меньше подписок наугад</h2>
          <p>
            Укажите, для чего вам нужен AI и сколько вы готовы тратить. Получите
            состав набора, расходы и проверяемые основания выбора.
          </p>
          <div className={builder.actions}>
            <Link className={builder.primary} to="/onboarding">
              Собрать мой AI-набор →
            </Link>
            <Link to="/catalog">Открыть каталог</Link>
          </div>
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
