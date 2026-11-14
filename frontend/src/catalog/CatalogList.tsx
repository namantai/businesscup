import type {
  PageProduct,
  PageAgent,
  PageAiModel,
} from '../api/generated/types.gen';
import { useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router';
import { readAgents, readMeta, readModels, readProducts } from '../api/catalog';
import { CatalogLayout, DateLabel, ErrorState, Loading } from './CatalogLayout';
import styles from './Catalog.module.css';

export function CatalogList() {
  const [params, setParams] = useSearchParams();
  const tab = ['models', 'agents'].includes(params.get('tab') ?? '')
    ? params.get('tab')!
    : 'products';
  const page = Math.max(0, Math.min(100000, Number(params.get('page')) || 0));
  const size = [2, 12, 24].includes(Number(params.get('size')))
    ? Number(params.get('size'))
    : 12;
  const q = params.get('q') ?? '';
  const kind = params.get('kind') ?? '';
  const capabilityId = params.get('capabilityId') ?? '';
  const sort = params.get('sort') ?? 'name';
  const metadata = useQuery({
    queryKey: ['catalog', 'meta'],
    queryFn: ({ signal }) => readMeta(signal),
  });
  const version = metadata.data?.versionId;
  const query = {
    version: version ?? undefined,
    q,
    page,
    size,
    sort,
    ...(tab === 'products'
      ? { kind: kind || undefined, capabilityId: capabilityId || undefined }
      : {}),
  };
  const entries = useQuery<PageProduct | PageAgent | PageAiModel>({
    queryKey: ['catalog', tab, query],
    queryFn: ({ signal }) =>
      tab === 'models'
        ? readModels(query, signal)
        : tab === 'agents'
          ? readAgents(query, signal)
          : readProducts(query, signal),
    enabled: !!version,
  });
  function change(key: string, value: string) {
    const next = new URLSearchParams(params);
    next.set(key, value);
    if (key !== 'page') next.delete('page');
    setParams(next, { replace: key === 'q' });
  }
  return (
    <CatalogLayout>
      <p className={styles.eyebrow}>ПРОДУКТЫ · МОДЕЛИ · АГЕНТЫ</p>
      <div className={styles.titleRow}>
        <div>
          <h1>Каталог AI</h1>
          <p className={styles.lead}>
            Проверьте возможности, условия и источники перед выбором подписки.
          </p>
        </div>
        <button
          onClick={() => {
            void metadata.refetch();
            void entries.refetch();
          }}
          disabled={metadata.isFetching}
        >
          Обновить каталог
        </button>
      </div>
      {metadata.isPending ? (
        <Loading />
      ) : metadata.isError ? (
        <ErrorState
          error={metadata.error}
          retry={() => void metadata.refetch()}
        />
      ) : (
        <>
          <p className={styles.coverage}>
            Продуктов: {metadata.data.productCount} · Регионов:{' '}
            {metadata.data.regions.length} · Неизвестных утверждений:{' '}
            {metadata.data.unknownClaims} · Устаревших:{' '}
            {metadata.data.staleClaims}. Покрытие рынка ограничено.
          </p>
          {metadata.data.datasetKind === 'DEMO' && (
            <p role="note" className={styles.warning}>
              Демонстрационный каталог: синтетические данные, не реальные цены.
            </p>
          )}
          {metadata.data.asOf && (
            <p className={styles.small}>
              Снимок от <DateLabel value={metadata.data.asOf} /> ·{' '}
              <span className={styles.version}>Версия: {version}</span>
            </p>
          )}
          <nav className={styles.tabs} aria-label="Вид каталога">
            {[
              ['products', 'Продукты'],
              ['models', 'Модели'],
              ['agents', 'Агенты'],
            ].map(([key, label]) => (
              <button
                key={key}
                aria-pressed={tab === key}
                onClick={() => change('tab', key)}
              >
                {label}
              </button>
            ))}
          </nav>
          <div className={styles.filters}>
            <label>
              Поиск по названию
              <input
                type="search"
                maxLength={200}
                value={q}
                onChange={(e) => change('q', e.target.value)}
                placeholder="Название инструмента"
              />
            </label>
            {tab === 'products' && (
              <>
                <label>
                  Тип продукта
                  <select
                    value={kind}
                    onChange={(e) => change('kind', e.target.value)}
                  >
                    <option value="">Все продукты</option>
                    <option value="APPLICATION">Приложения</option>
                    <option value="AUTOMATION_TOOL">
                      Инструменты автоматизации
                    </option>
                  </select>
                </label>
                <label>
                  Описанная возможность
                  <select
                    value={capabilityId}
                    onChange={(e) => change('capabilityId', e.target.value)}
                  >
                    <option value="">Все возможности</option>
                    {metadata.data.capabilities.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            )}
            <label>
              Порядок
              <select
                value={sort}
                onChange={(e) => change('sort', e.target.value)}
              >
                <option value="name">Название А–Я</option>
                <option value="-name">Название Я–А</option>
              </select>
            </label>
            <label>
              На странице
              <select
                value={size}
                onChange={(e) => change('size', e.target.value)}
              >
                <option value="2">2</option>
                <option value="12">12</option>
                <option value="24">24</option>
              </select>
            </label>
          </div>
          {capabilityId && tab === 'products' && (
            <p className={styles.small}>
              Фильтр ищет описанные свойства, включая UNKNOWN. Поддержку и
              условия проверяйте в карточке.
            </p>
          )}
          {!version ? (
            <p role="status">
              Каталог пока не опубликован. Проверенные данные появятся после
              публикации.
            </p>
          ) : entries.isPending ? (
            <Loading />
          ) : entries.isError ? (
            <ErrorState
              error={entries.error}
              retry={() => void entries.refetch()}
            />
          ) : (
            <>
              <p role="status">Найдено: {entries.data.total}</p>
              {entries.data.items.length === 0 ? (
                <p>Ничего не найдено. Измените поиск или фильтры.</p>
              ) : (
                <div className={styles.grid}>
                  {entries.data.items.map((item) => (
                    <article className={styles.card} key={item.id}>
                      <p className={styles.eyebrow}>
                        {'slug' in item
                          ? item.kind === 'APPLICATION'
                            ? 'ПРИЛОЖЕНИЕ'
                            : 'ИНСТРУМЕНТ АВТОМАТИЗАЦИИ'
                          : 'exactVersion' in item
                            ? 'МОДЕЛЬ'
                            : 'АГЕНТ'}
                      </p>
                      <h2>
                        {'slug' in item ? (
                          <Link
                            to={`/catalog/${encodeURIComponent(item.slug)}?version=${version}`}
                          >
                            {item.name}
                          </Link>
                        ) : (
                          item.name
                        )}
                      </h2>
                      {'exactVersion' in item && (
                        <p>
                          Версия модели: {item.exactVersion}. Это технология, а
                          не подписка.
                        </p>
                      )}
                      {'executionEnvironment' in item && (
                        <p>
                          {item.executionEnvironment}. Доступ и включение
                          проверяйте в тарифе продукта.
                        </p>
                      )}
                      {'slug' in item && (
                        <p>Функции, тарифы, ограничения и источники →</p>
                      )}
                    </article>
                  ))}
                </div>
              )}
              <nav className={styles.pagination} aria-label="Страницы каталога">
                <button
                  disabled={page === 0}
                  onClick={() => change('page', String(page - 1))}
                >
                  Назад
                </button>
                <span>
                  Страница {page + 1} из{' '}
                  {Math.max(1, Math.ceil(entries.data.total / size))}
                </span>
                <button
                  disabled={(page + 1) * size >= entries.data.total}
                  onClick={() => change('page', String(page + 1))}
                >
                  Далее
                </button>
              </nav>
            </>
          )}
        </>
      )}
    </CatalogLayout>
  );
}
