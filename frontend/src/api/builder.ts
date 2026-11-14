import './catalog';
import {
  products,
  plans,
  offers,
  models,
  agents,
  capabilities,
  needs,
  usageLimits,
  csrf,
  preview,
} from './generated/sdk.gen';
import type { RecommendationProfile } from './generated/types.gen';

async function all<T>(
  load: (
    page: number,
  ) => Promise<{ items: T[]; total: number; versionId: string | null }>,
  version: string,
) {
  const items: T[] = [];
  for (let page = 0; ; page++) {
    const data = await load(page);
    if (data.versionId !== version)
      throw new Error('Catalog snapshot mismatch');
    items.push(...data.items);
    if (items.length >= data.total) return items;
    if (!data.items.length || page >= 100)
      throw new Error('Incomplete catalog');
  }
}
export async function readBuilderCatalog(version: string, signal: AbortSignal) {
  const query = (page: number) => ({ version, page, size: 100 });
  const [
    productRows,
    planRows,
    offerRows,
    modelRows,
    agentRows,
    capabilityRows,
    needRows,
    limitRows,
  ] = await Promise.all([
    all(
      (p) =>
        products({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        plans({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        offers({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        models({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        agents({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        capabilities({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        needs({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
    all(
      (p) =>
        usageLimits({ query: query(p), signal, throwOnError: true }).then(
          (r) => r.data,
        ),
      version,
    ),
  ]);
  return {
    version,
    products: productRows,
    plans: planRows,
    offers: offerRows.map((o) => o.terms),
    models: modelRows,
    agents: agentRows,
    capabilities: capabilityRows,
    needs: needRows,
    limits: limitRows,
  };
}
export type BuilderCatalog = Awaited<ReturnType<typeof readBuilderCatalog>>;
export class PreviewError extends Error {
  constructor(
    public status: number,
    public retryAfter = 0,
  ) {
    super('Preview request failed');
  }
}
export async function requestPreview(
  body: RecommendationProfile,
  signal: AbortSignal,
) {
  const token = await csrf({ signal });
  if (!token.data)
    throw new PreviewError(
      token.response?.status ?? 0,
      retrySeconds(token.response),
    );
  const result = await preview({
    body,
    signal,
    headers: { [token.data.headerName]: token.data.token },
  });
  if (!result.data)
    throw new PreviewError(
      result.response?.status ?? 0,
      retrySeconds(result.response),
    );
  return result.data;
}
function retrySeconds(response: Response | undefined) {
  if (!response) return 0;
  const value = response.headers.get('Retry-After');
  if (!value) return response.status === 429 ? 60 : 0;
  const seconds = /^\d+$/.test(value)
    ? Number(value)
    : Math.ceil((Date.parse(value) - Date.now()) / 1000);
  return Number.isFinite(seconds) ? Math.max(0, seconds) : 60;
}
export function errorMessage(error: unknown) {
  const status =
    error instanceof PreviewError
      ? error.status
      : typeof error === 'object' && error && 'status' in error
        ? error.status
        : 0;
  switch (status) {
    case 400:
      return 'Проверьте параметры: сервер не принял профиль. Уточните суммы, даты продлений и выбранные тарифы.';
    case 403:
      return 'Сессия не подтверждена. Повторите запрос, чтобы получить новый защитный токен.';
    case 409:
      return 'Каталог изменился во время подбора. Обновите каталог и проверьте выбранные параметры.';
    case 413:
      return 'Анкета слишком большая. Сократите список подписок и дополнительных условий.';
    case 429:
      return 'Слишком много запросов. Дождитесь окончания паузы и повторите подбор.';
    case 500:
      return 'Сервис временно не смог выполнить подбор. Ваши параметры сохранены в этой вкладке.';
    default:
      return 'Не удалось получить ответ. Проверьте соединение и повторите попытку. Ваши параметры сохранены в этой вкладке.';
  }
}
