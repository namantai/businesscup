import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { CatalogLayout } from '../catalog/CatalogLayout';
import {
  validAmount,
  humanGoalNames,
  pickBundle,
  presets,
  pricePresentation,
  quickMatches,
  type Catalog,
  type Claim,
  type Evidence,
  type Match,
  type Need,
  type NeedMap,
  type Offer,
  type Plan,
  type Product,
  type Source,
} from './matching';
import styles from './QuickMatch.module.css';

type CatalogPage<T> = { versionId: string | null; items: T[]; total: number };
type Meta = { versionId: string | null; needs: Need[]; status: string };
async function loadPages<T>(
  path: string,
  version: string,
  signal: AbortSignal,
): Promise<T[]> {
  const items: T[] = [];
  for (let page = 0; page < 24; page++) {
    const query = new URLSearchParams({
      version,
      page: String(page),
      size: '50',
    });
    const response = await fetch(
      `/api/v1/catalog/${path}?${query.toString()}`,
      {
        credentials: 'same-origin',
        signal,
      },
    );
    if (!response.ok)
      throw new Error(`Каталог ${path}: HTTP ${response.status}`);
    const data = (await response.json()) as CatalogPage<T>;
    if (data.versionId !== version)
      throw new Error('Версия каталога изменилась');
    items.push(...data.items);
    if (items.length >= data.total) return items;
    if (!data.items.length)
      throw new Error(`Каталог ${path}: неполная страница`);
  }
  throw new Error(`Каталог ${path}: слишком много страниц`);
}
async function loadCatalog(signal: AbortSignal): Promise<Catalog> {
  const r = await fetch('/api/v1/catalog/meta', {
    credentials: 'same-origin',
    signal,
  });
  if (!r.ok) throw new Error(`Не удалось получить каталог: HTTP ${r.status}`);
  const meta = (await r.json()) as Meta;
  if (!meta.versionId || meta.status !== 'AVAILABLE')
    throw new Error('Каталог не опубликован');
  const version = meta.versionId;
  const [products, plans, offers, claims, mappings, evidence, sources] =
    await Promise.all([
      loadPages<Product>('products', version, signal),
      loadPages<Plan>('plans', version, signal),
      loadPages<Offer>('offers', version, signal),
      loadPages<Claim>('assertions', version, signal),
      loadPages<NeedMap>('need-capabilities', version, signal),
      loadPages<Evidence>('evidence', version, signal),
      loadPages<Source>('sources', version, signal),
    ]);
  return {
    products,
    plans,
    offers,
    claims,
    mappings,
    evidence,
    sources,
    needs: meta.needs,
    version,
  };
}
const label = (id: string, data: Catalog | null) =>
  humanGoalNames[id] || data?.needs.find((n) => n.id === id)?.name || id;
function PriceLine({
  match,
  country,
  currency,
}: {
  match: Match;
  country: string;
  currency: string;
}) {
  const info = pricePresentation(match, country, currency);
  return (
    <div className={styles.priceBlock}>
      <strong className={styles.price}>{info.headline}</strong>
      <span className={styles.priceScope}>{info.detail}</span>
      {info.price?.sourceUrl ? (
        <a
          className={styles.source}
          href={info.price.sourceUrl}
          target="_blank"
          rel="noreferrer"
        >
          Источник ·{' '}
          {info.price.checkedAt
            ? new Date(info.price.checkedAt).toLocaleDateString('ru-RU')
            : 'дата неизвестна'}
        </a>
      ) : (
        <span className={styles.priceScope}>
          Цена по источнику не подтверждена
        </span>
      )}
    </div>
  );
}
export function QuickMatchPage() {
  const [data, setData] = useState<Catalog | null>(null);
  const [asOf, setAsOf] = useState(() => new Date());
  const [loadError, setLoadError] = useState('');
  const [retry, setRetry] = useState(0);
  const [role, setRole] = useState('student-dev');
  const [customized, setCustomized] = useState(false);
  const [goals, setGoals] = useState<string[]>([
    'study',
    'development',
    'presentations',
    'research',
  ]);
  const [country, setCountry] = useState('KZ');
  const [budget, setBudget] = useState('20');
  const [currency, setCurrency] = useState('USD');
  const [query, setQuery] = useState('');
  const [showAll, setShowAll] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    loadCatalog(controller.signal)
      .then((catalog) => {
        if (controller.signal.aborted) return;
        setAsOf(new Date());
        setData(catalog);
        setLoadError('');
      })
      .catch((error) => {
        if (!controller.signal.aborted)
          setLoadError(
            error instanceof Error
              ? error.message
              : 'Не удалось загрузить каталог',
          );
      });
    return () => controller.abort();
  }, [retry]);
  const available = data?.needs || [];
  const availableGoals = new Set(available.map((n) => n.id));
  const chosen = goals.filter((g) => availableGoals.has(g));
  const validBudget = validAmount(budget);
  const matches = data
    ? quickMatches(
        data,
        chosen,
        validBudget ? budget : null,
        currency,
        country,
        asOf,
      )
    : [];
  const relevant = matches.filter((m) => m.matches.length > 0);
  const bundle = pickBundle(matches, chosen);
  const listed = matches.filter((m) =>
    m.product.name.toLowerCase().includes(query.trim().toLowerCase()),
  );
  const currentPreset = presets.find((p) => p.id === role);
  function choosePreset(id: string) {
    const p = presets.find((x) => x.id === id);
    if (!p) return;
    setRole(id);
    setGoals(p.needs.filter((n) => availableGoals.has(n)));
    setCustomized(false);
  }
  function toggle(id: string) {
    setGoals((old) =>
      old.includes(id) ? old.filter((x) => x !== id) : [...old, id],
    );
    setCustomized(true);
  }
  return (
    <CatalogLayout>
      <div className={styles.page}>
        <header className={styles.top}>
          <div>
            <span className={styles.kicker}>AI Compass</span>
            <h1>Найди AI под свои задачи.</h1>
            <p>Выбери несколько целей и бюджет. Варианты появятся сразу.</p>
          </div>
          <span className={styles.catalogCount}>
            {data
              ? `${data.products.length} AI-сервиса в каталоге`
              : 'Загружаем каталог'}
          </span>
        </header>
        {loadError ? (
          <div role="alert" className={styles.error}>
            Не удалось загрузить данные. {loadError}{' '}
            <button type="button" onClick={() => setRetry((v) => v + 1)}>
              Повторить
            </button>
          </div>
        ) : null}
        {!data && !loadError ? (
          <div role="status" className={styles.loading}>
            Загружаем цены и возможности…
          </div>
        ) : null}
        {data ? (
          <>
            <div className={styles.columns}>
              <section
                className={styles.controlPanel}
                aria-label="Выбор целей и бюджета"
              >
                <div className={styles.sectionHead}>
                  <h2>Быстрый старт</h2>
                  <span className={styles.small}>Необязательно</span>
                </div>
                <div className={styles.presetRow}>
                  {presets.map((p) => (
                    <button
                      type="button"
                      key={p.id}
                      className={`${styles.preset} ${role === p.id ? styles.selected : ''}`}
                      aria-pressed={role === p.id}
                      onClick={() => choosePreset(p.id)}
                    >
                      {p.title}
                    </button>
                  ))}
                </div>
                {currentPreset ? (
                  <p className={styles.presetNote}>
                    Основа: {currentPreset.title}
                    {customized ? ' · цели изменены вручную' : ''}
                  </p>
                ) : null}
                <div className={styles.sectionHead}>
                  <h2>Для чего тебе AI?</h2>
                  <span className={styles.small}>Можно несколько</span>
                </div>
                <div className={styles.chips} aria-label="Выбор целей">
                  {available.map((n) => (
                    <button
                      type="button"
                      key={n.id}
                      className={`${styles.chip} ${chosen.includes(n.id) ? styles.selected : ''}`}
                      aria-pressed={chosen.includes(n.id)}
                      onClick={() => toggle(n.id)}
                    >
                      <span aria-hidden="true">
                        {chosen.includes(n.id) ? '✓' : '+'}
                      </span>{' '}
                      {label(n.id, data)}
                    </button>
                  ))}
                </div>
                <div className={styles.budgetRow}>
                  <label className={styles.budgetField}>
                    <span>Бюджет в месяц</span>
                    <div className={styles.moneyInput}>
                      <input
                        type="text"
                        inputMode="decimal"
                        value={budget}
                        onChange={(e: { target: { value: string } }) =>
                          setBudget(e.target.value)
                        }
                        aria-label="Месячный бюджет"
                      />
                      <select
                        value={currency}
                        onChange={(e: { target: { value: string } }) =>
                          setCurrency(e.target.value)
                        }
                        aria-label="Валюта бюджета"
                      >
                        <option value="USD">USD · $</option>
                        <option value="KZT">KZT · ₸</option>
                        <option value="CNY">CNY · ¥</option>
                      </select>
                    </div>
                  </label>
                  <label className={styles.countryField}>
                    <span>Страна</span>
                    <select
                      value={country}
                      onChange={(e: { target: { value: string } }) =>
                        setCountry(e.target.value)
                      }
                    >
                      <option value="KZ">Казахстан</option>
                      <option value="US">США</option>
                      <option value="CN">Китай</option>
                    </select>
                  </label>
                </div>
                <div
                  className={styles.budgetShortcuts}
                  aria-label="Быстрый выбор бюджета"
                >
                  {[0, 10, 20, 50, 100].map((n) => (
                    <button
                      type="button"
                      key={n}
                      onClick={() => setBudget(String(n))}
                      className={
                        budget === String(n) ? styles.shortcutActive : ''
                      }
                    >
                      {n === 0 ? 'Бесплатно' : n}
                    </button>
                  ))}
                </div>
                {!validBudget ? (
                  <p role="alert" className={styles.error}>
                    Укажи 0 или положительную сумму: до 12 цифр и 6 знаков после
                    точки.
                  </p>
                ) : null}
                <p className={styles.small}>
                  Результаты ниже обновляются сразу. Точная доступность подписки
                  и качество моделей отдельно не проверены.
                </p>
                <details className={styles.advanced}>
                  <summary>Нужен более точный подбор?</summary>
                  <p>
                    В расширенной анкете можно учесть платформу, подписки,
                    ограничения и язык.
                  </p>
                  <Link to="/onboarding">Перейти к расширенному подбору</Link>
                </details>
              </section>
              <aside
                className={styles.preview}
                aria-live="polite"
                aria-atomic="false"
              >
                <p className={styles.previewLabel}>
                  ВАРИАНТ НАБОРА ПО ФУНКЦИЯМ
                </p>
                <h2>
                  {bundle.items.length > 0
                    ? `${bundle.items.length} ${bundle.items.length === 1 ? 'инструмент' : 'инструмента'} для твоих целей`
                    : 'Выбери задачи'}
                </h2>
                <p className={styles.small}>
                  {bundle.items.length > 0
                    ? 'По подтверждённым функциям. Общая стоимость и совместимость не рассчитаны; это не гарантия покупки или качества.'
                    : 'Отметь хотя бы одну цель выше.'}
                </p>
                {bundle.items.map((m) => (
                  <div key={m.product.id} className={styles.bundleItem}>
                    <strong>{m.product.name}</strong>
                    <span>
                      {m.matches
                        .filter((g) => chosen.includes(g))
                        .map((g) => label(g, data))
                        .join(' · ')}
                    </span>
                    <PriceLine
                      match={m}
                      country={country}
                      currency={currency}
                    />
                  </div>
                ))}
                {bundle.missing.length > 0 && chosen.length > 0 ? (
                  <p className={styles.gap}>
                    Не удалось подтвердить:{' '}
                    {bundle.missing.map((id) => label(id, data)).join(', ')}
                  </p>
                ) : null}
                {bundle.items.length > 0 ? (
                  <p className={styles.note}>
                    Цены разных валют не складываем. Регион покупки, налоги и
                    лимиты могут потребовать проверки.
                  </p>
                ) : null}
              </aside>
            </div>
            <section
              className={styles.results}
              aria-labelledby="matching-title"
            >
              <div className={styles.resultsHeader}>
                <div>
                  <h2 id="matching-title">Подходящие по задачам</h2>
                  <p className={styles.small}>
                    Сначала — самое широкое подтверждённое покрытие твоих целей,
                    а не рекламный рейтинг.
                  </p>
                </div>
                <span className={styles.resultCount}>
                  {relevant.length} с совпадениями
                </span>
              </div>
              {!chosen.length ? (
                <p>Отметь хотя бы одну цель, чтобы увидеть варианты.</p>
              ) : !relevant.length ? (
                <p>
                  Подтверждённых совпадений по этим целям пока нет. Ниже можно
                  изучить весь каталог.
                </p>
              ) : null}
              <div className={styles.resultCards}>
                {relevant.slice(0, showAll ? relevant.length : 6).map((m) => {
                  const needs = m.matches.map((id) => label(id, data));
                  return (
                    <article key={m.product.id} className={styles.matchCard}>
                      <div className={styles.cardTop}>
                        <span className={styles.avatar}>
                          {m.product.name.slice(0, 2).toUpperCase()}
                        </span>
                        <span className={styles.countPill}>
                          {m.matches.length} из {chosen.length} целей
                        </span>
                      </div>
                      <h3>{m.product.name}</h3>
                      <p className={styles.matchTags}>{needs.join(' · ')}</p>
                      <PriceLine
                        match={m}
                        country={country}
                        currency={currency}
                      />
                      <p className={styles.limitation}>
                        {m.budgetFit === 'above-budget'
                          ? 'Базовая цена выше бюджета'
                          : m.budgetFit === 'not-comparable'
                            ? 'Сопоставление с бюджетом требует проверки'
                            : 'Базовая цена — не полный платёж'}
                      </p>
                      <Link
                        className={styles.cardLink}
                        to={`/catalog/${encodeURIComponent(m.product.slug)}?version=${encodeURIComponent(data.version)}`}
                      >
                        Тарифы и условия <span aria-hidden="true">↗</span>
                      </Link>
                    </article>
                  );
                })}
              </div>
              {relevant.length > 6 ? (
                <button
                  className={styles.moreButton}
                  type="button"
                  onClick={() => setShowAll((v) => !v)}
                >
                  {showAll ? 'Свернуть' : 'Показать ещё подходящие'}
                </button>
              ) : null}
            </section>
            <section className={styles.directory} aria-labelledby="all-title">
              <div className={styles.resultsHeader}>
                <div>
                  <h2 id="all-title">Весь каталог</h2>
                  <p className={styles.small}>
                    Все продукты остаются доступны, даже без подтверждённой цены
                    или функции.
                  </p>
                </div>
                <span className={styles.resultCount}>
                  {matches.length} продуктов
                </span>
              </div>
              <label className={styles.search}>
                <span className={styles.srOnly}>Поиск по названию</span>
                <input
                  type="search"
                  value={query}
                  onChange={(e: { target: { value: string } }) =>
                    setQuery(e.target.value)
                  }
                  placeholder="Поиск: Claude, Kimi, Higgsfield…"
                />
              </label>
              <div className={styles.directoryList}>
                {listed.map((m) => (
                  <Link
                    className={styles.directoryRow}
                    to={`/catalog/${encodeURIComponent(m.product.slug)}?version=${encodeURIComponent(data.version)}`}
                    key={m.product.id}
                  >
                    <strong>{m.product.name}</strong>
                    <span>
                      {chosen.length
                        ? `${m.matches.length}/${chosen.length} целей`
                        : 'Подробнее'}
                    </span>
                    <b>{pricePresentation(m, country, currency).headline}</b>
                    <span aria-hidden="true">↗</span>
                  </Link>
                ))}
              </div>
              {listed.length === 0 ? (
                <p>Ничего не найдено. Попробуй другое название.</p>
              ) : null}
            </section>
            <p className={styles.footerNote}>
              Каталог: {data.products.length} продуктов. Сервис сравнивает
              опубликованные функции и базовые цены, но не тестирует качество
              AI-моделей и не подтверждает покупку в твоей стране.{' '}
              <Link to="/methodology">Как устроен подбор</Link>
            </p>
          </>
        ) : null}
      </div>
    </CatalogLayout>
  );
}
