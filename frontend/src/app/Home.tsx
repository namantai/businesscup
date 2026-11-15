import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { Navigation } from './Navigation';
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
        <section className={styles.hero}>
          <div className={styles.heroCopy}>
            <p className={styles.eyebrow}>МЕНЬШЕ ПОИСКА. БОЛЬШЕ ДЕЛА.</p>
            <h1>
              Найди AI
              <br />
              под свои
              <br />
              <em>задачи.</em>
            </h1>
            <p className={styles.intro}>
              Подберём нейросети, агенты и подписки с учётом твоих целей и
              бюджета.
            </p>
            <div className={styles.ctas}>
              <Link className={styles.cta} to="/onboarding">
                Подобрать AI <span aria-hidden="true">↗</span>
              </Link>
              <Link className={styles.textLink} to="/catalog">
                Смотреть каталог →
              </Link>
            </div>
            <p className={styles.fine}>
              Без ввода конкретного задания. Без регистрации.
            </p>
          </div>
          <div
            className={styles.visual}
            aria-label="Иллюстрация AI-набора, не рекомендация"
          >
            <div className={styles.orbit} aria-hidden="true">
              <span>✳</span>
            </div>
            <div className={styles.stack}>
              <div className={styles.stackTop}>
                <span>ТВОЙ AI-НАБОР</span>
                <span aria-hidden="true">↗</span>
              </div>
              <h2>
                Разные цели.
                <br />
                Один набор.
              </h2>
              <div className={styles.tool}>
                <span className={styles.toolIcon}>C</span>
                <div>
                  <strong>ChatGPT</strong>
                  <small>Пример AI-продукта</small>
                </div>
                <span aria-hidden="true">↗</span>
              </div>
              <div className={styles.tool}>
                <span className={styles.toolIcon}>Cl</span>
                <div>
                  <strong>Claude</strong>
                  <small>Пример AI-продукта</small>
                </div>
                <span aria-hidden="true">↗</span>
              </div>
              <div className={styles.tool}>
                <span className={styles.toolIcon}>G</span>
                <div>
                  <strong>GitHub Copilot</strong>
                  <small>Пример AI-продукта</small>
                </div>
                <span aria-hidden="true">↗</span>
              </div>
              <p className={styles.illustration}>
                Иллюстрация, не рекомендация.
                <br />
                Твой набор зависит от целей и проверенных данных.
              </p>
            </div>
            <span className={styles.visualCaption}>
              ПОД ТВОЙ РИТМ. В ТВОЙ БЮДЖЕТ.
            </span>
          </div>
        </section>
        <section className={styles.how} aria-labelledby="how-title">
          <div>
            <p className={styles.eyebrow}>ПРОСТО НАЧНИ</p>
            <h2 id="how-title">Три шага к своему набору.</h2>
          </div>
          <div className={styles.howGrid}>
            <article>
              <span>01</span>
              <h3>Выбери задачи</h3>
              <p>Учёба, код или творчество. Можно несколько сразу.</p>
            </article>
            <article>
              <span>02</span>
              <h3>Укажи бюджет</h3>
              <p>Учтём ограничения и подписки, которые уже есть.</p>
            </article>
            <article>
              <span>03</span>
              <h3>Сравни варианты</h3>
              <p>
                Состав, стоимость, источники. Пробелы в данных тоже покажем.
              </p>
            </article>
          </div>
        </section>
        <div className={styles.service}>
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
            Проверить подключение ↻
          </button>
        </div>
      </main>
      <footer className={styles.footer}>
        <span>AI Compass · Осознанный выбор</span>
        <Link to="/methodology">Как это работает</Link>
      </footer>
    </div>
  );
}
