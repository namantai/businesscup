import { afterEach, expect, test, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';

const version = 'synthetic-quick-match';
const date = '2026-01-01T00:00:00Z';
const needs = ['study', 'development', 'presentations', 'research'].map(
  (id) => ({ id, name: id }),
);
const products = [
  'Synthetic study',
  'Synthetic slides',
  'Synthetic unknown',
].map((name, i) => ({
  id: String(i),
  slug: String(i),
  name,
  state: 'ACTIVE',
  kind: 'APPLICATION',
  providerId: 'synthetic',
}));
const plans = products.slice(0, 2).map((p) => ({
  id: p.id,
  productId: p.id,
  tier: 'Free',
  accessType: 'APPLICATION',
  activeFrom: date,
  activeUntil: null,
}));
const rows: Record<string, unknown[]> = {
  products,
  plans,
  offers: plans.map((p) => ({
    terms: {
      id: p.id,
      planId: p.id,
      regionId: 'us',
      currency: 'USD',
      channel: 'WEB',
      priceKind: 'FREE',
      billingPeriod: 'MONTH',
      amount: '0',
      upfrontAmount: '0',
      priceEvidenceId: 'price',
      validFrom: date,
      validUntil: null,
    },
  })),
  assertions: needs.map((n) => ({
    id: n.id,
    productId: null,
    planId: n.id === 'presentations' ? '1' : '0',
    capabilityId: n.id,
    state: 'SUPPORTED',
    evidenceId: 'verified',
  })),
  'need-capabilities': needs.map((n) => ({ needId: n.id, capabilityId: n.id })),
  evidence: [
    {
      id: 'verified',
      sourceId: 'source',
      verifiedAt: date,
      effectiveStatus: 'VERIFIED_SOURCE',
    },
    {
      id: 'price',
      sourceId: 'source',
      verifiedAt: date,
      effectiveStatus: 'CONDITIONAL',
    },
  ],
  sources: [
    {
      id: 'source',
      publisher: 'Synthetic official',
      url: 'https://example.org/pricing',
    },
  ],
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status });
function backend(options: { fail?: boolean; mismatch?: boolean } = {}) {
  const fetcher = vi.fn(async (input: Request | string) => {
    const url = new URL(
      typeof input === 'string' ? input : input.url,
      window.location.origin,
    );
    if (url.pathname === '/actuator/health/readiness')
      return json({ status: 'UP' });
    const key = url.pathname.split('/').at(-1)!;
    if (key === 'meta')
      return options.fail
        ? json({}, 503)
        : json({ versionId: version, status: 'AVAILABLE', needs });
    const data = rows[key] ?? [];
    // Two small pages exercise complete version-pinned loading, including offers/evidence.
    const page = Number(url.searchParams.get('page'));
    return json({
      versionId: options.mismatch ? 'wrong' : version,
      items: data.slice(page * 2, page * 2 + 2),
      total: data.length,
    });
  });
  vi.stubGlobal('fetch', fetcher);
  window.history.replaceState({}, '', '/');
  render(<App />);
  return fetcher;
}
afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', '/');
});

test('loads all pages, ranks source-backed functions and preserves unknown products on home', async () => {
  const fetcher = backend();
  const directory = await screen.findByRole('region', { name: 'Весь каталог' });
  expect(within(directory).getAllByRole('link')).toHaveLength(3);
  const matches = screen.getByRole('region', { name: 'Подходящие по задачам' });
  expect(within(matches).getAllByRole('article')).toHaveLength(2);
  expect(within(matches).getByText('3 из 4 целей')).toBeVisible();
  expect(within(matches).getAllByText(/не цена KZ/)).toHaveLength(2);
  expect(within(directory).getByText('Цена не подтверждена')).toBeVisible();
  expect(
    fetcher.mock.calls.every(
      ([r]) =>
        String(r).endsWith('/meta') || String(r).includes(`version=${version}`),
    ),
  ).toBe(true);
  expect(fetcher.mock.calls.some(([r]) => String(r).includes('page=1'))).toBe(
    true,
  );
  await userEvent.click(screen.getByRole('button', { name: 'Презентации' }));
  expect(screen.getByRole('button', { name: 'Студент + код' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(
    screen.getByText(/Основа: Студент \+ код · цели изменены вручную/),
  ).toBeVisible();
  await userEvent.clear(screen.getByLabelText('Месячный бюджет'));
  expect(screen.getByRole('alert')).toBeVisible();
  expect(within(directory).getAllByRole('link')).toHaveLength(3);
  await userEvent.type(screen.getByLabelText('Месячный бюджет'), '20');
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  await userEvent.type(screen.getByLabelText('Поиск по названию'), 'unknown');
  expect(within(directory).getAllByRole('link')).toHaveLength(1);
});
test('recovers a failed catalog load without invented results', async () => {
  const options = { fail: true };
  backend(options);
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Не удалось загрузить данные',
  );
  expect(
    screen.queryByRole('region', { name: 'Весь каталог' }),
  ).not.toBeInTheDocument();
  options.fail = false;
  await userEvent.click(screen.getByRole('button', { name: 'Повторить' }));
  expect(
    await screen.findByRole('region', { name: 'Весь каталог' }),
  ).toBeVisible();
});
test('rejects a mixed snapshot instead of blending published versions', async () => {
  backend({ mismatch: true });
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Версия каталога изменилась',
  );
  expect(
    screen.queryByRole('region', { name: 'Весь каталог' }),
  ).not.toBeInTheDocument();
});

test('quick and advanced navigation preserves only the mounted app draft without auto preview', async () => {
  const storage = vi.spyOn(Storage.prototype, 'setItem');
  const fetcher = backend();
  await screen.findByRole('region', { name: 'Весь каталог' });
  const nav = within(
    screen.getByRole('navigation', { name: 'Основная навигация' }),
  );
  expect(nav.getByRole('link', { name: 'Быстрый подбор' })).toHaveAttribute(
    'href',
    '/',
  );
  expect(nav.getByRole('link', { name: 'Быстрый подбор' })).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(nav.getByRole('link', { name: 'Точный подбор' })).toHaveAttribute(
    'href',
    '/onboarding',
  );
  expect(nav.getByRole('link', { name: 'Каталог' })).toHaveAttribute(
    'href',
    '/catalog',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Презентации' }));
  await userEvent.clear(screen.getByLabelText('Месячный бюджет'));
  await userEvent.type(screen.getByLabelText('Месячный бюджет'), '42.50');
  await userEvent.selectOptions(screen.getByLabelText('Валюта бюджета'), 'CNY');
  await userEvent.click(
    screen.getByRole('link', { name: 'Нужен точный подбор?' }),
  );
  expect(screen.getByRole('link', { name: 'Точный подбор' })).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(
    screen.getByRole('link', { name: 'Быстрый подбор' }),
  ).not.toHaveAttribute('aria-current');
  await userEvent.click(
    screen.getByRole('link', { name: '← Вернуться к быстрому подбору' }),
  );
  await screen.findByRole('region', { name: 'Весь каталог' });
  expect(screen.getByLabelText('Месячный бюджет')).toHaveValue('42.50');
  expect(screen.getByLabelText('Валюта бюджета')).toHaveValue('CNY');
  expect(screen.getByRole('button', { name: 'Презентации' })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
  expect(screen.getByRole('button', { name: 'Студент + код' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  await userEvent.click(screen.getByRole('link', { name: 'Точный подбор' }));
  await userEvent.click(screen.getByRole('link', { name: 'Быстрый подбор' }));
  await screen.findByRole('region', { name: 'Весь каталог' });
  expect(screen.getByLabelText('Месячный бюджет')).toHaveValue('42.50');
  expect(storage).not.toHaveBeenCalled();
  expect(
    fetcher.mock.calls.some(([r]) =>
      String(typeof r === 'string' ? r : r.url).includes(
        '/recommendations/preview',
      ),
    ),
  ).toBe(false);
});
