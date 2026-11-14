import { client } from './generated/client.gen';
import { agents, detail, meta, models, products } from './generated/sdk.gen';
import type { ProductsData } from './generated/types.gen';

client.setConfig({
  baseUrl: window.location.origin,
  credentials: 'same-origin',
});
export const readMeta = (signal: AbortSignal) =>
  meta({ signal, throwOnError: true }).then((r) => r.data);
export const readProducts = (
  query: ProductsData['query'],
  signal: AbortSignal,
) => products({ query, signal, throwOnError: true }).then((r) => r.data);
export const readModels = (query: ProductsData['query'], signal: AbortSignal) =>
  models({ query, signal, throwOnError: true }).then((r) => r.data);
export const readAgents = (query: ProductsData['query'], signal: AbortSignal) =>
  agents({ query, signal, throwOnError: true }).then((r) => r.data);
export const readDetail = (
  slug: string,
  version: string,
  signal: AbortSignal,
) =>
  detail({
    path: { slug },
    query: { version },
    signal,
    throwOnError: true,
  }).then((r) => r.data);
