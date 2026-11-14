import { beforeEach, afterEach, test, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { App } from '../app/App';
import { Comparison } from './SubscriptionPage';
import { emptySubscription } from './form';
import { catalogResponse, result } from '../builder/test-data';
import type {
  OptimizationResult,
  SubscriptionEntry,
} from '../api/generated/types.gen';
const response = (data: unknown, status = 200) =>
  new Response(status === 204 ? null : JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
let rows: SubscriptionEntry[];
let failures: number;
let writes: Array<Record<string, unknown>>;
let pendingWrite: Promise<void> | undefined;
beforeEach(() => {
  history.replaceState({}, '', '/subscriptions');
  rows = [];
  failures = 0;
  writes = [];
  pendingWrite = undefined;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request | string) => {
      const req = typeof input === 'string' ? new Request(input) : input;
      const url = new URL(req.url);
      const path = url.pathname;
      if (path.endsWith('/auth/session'))
        return response({
          account: {
            id: 'owner-a',
            email: 'a@example.invalid',
            role: 'USER',
            verified: true,
            plan: 'FREE',
            createdAt: '2026-11-14T12:00:00Z',
          },
        });
      if (path.endsWith('/auth/csrf'))
        return response({
          headerName: 'X-CSRF-TOKEN',
          token: 'synthetic-only',
        });
      if (path.endsWith('/catalog/evidence'))
        return response({
          items: result().evidence,
          total: result().evidence.length,
          page: 0,
          size: 100,
          versionId: result().catalogVersion,
        });
      if (path.includes('/catalog/')) return response(catalogResponse(url));
      if (path.endsWith('/profiles')) return response([]);
      if (path.includes('/subscriptions')) {
        expect(req.headers.get('X-Compass-Account')).toBe('owner-a');
        if (req.method === 'PATCH') await pendingWrite;
        if (failures) return response({ status: failures }, failures);
        if (req.method === 'GET') return response(rows);
        expect(req.headers.get('X-CSRF-TOKEN')).toBe('synthetic-only');
        if (req.method === 'DELETE') {
          rows = [];
          return response(null, 204);
        }
        const body = await req.json();
        writes.push(body);
        const entry = {
          id: 'one',
          terms: body.terms,
          revision: 1,
          createdAt: '',
          updatedAt: '',
        };
        rows = [entry];
        return response(entry, req.method === 'POST' ? 201 : 200);
      }
      throw new Error('Unexpected test request');
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
test('empty private ledger and unknown defaults, no browser storage', async () => {
  const storage = vi.spyOn(Storage.prototype, 'setItem');
  render(<App />);
  expect(await screen.findByText(/Подписок пока нет/)).toBeVisible();
  await userEvent.click(
    screen.getByRole('button', { name: 'Добавить подписку' }),
  );
  expect(screen.getByLabelText('Фактическая цена за период')).toHaveValue('');
  expect(screen.getByLabelText('Автопродление')).toHaveValue('UNKNOWN');
  expect(storage).not.toHaveBeenCalled();
  storage.mockRestore();
});
test('custom actual price is USER_REPORTED, uses CSRF and server account precondition', async () => {
  render(<App />);
  await screen.findByText(/Подписок пока нет/);
  await userEvent.click(
    screen.getByRole('button', { name: 'Добавить подписку' }),
  );
  await userEvent.type(screen.getByLabelText('Название'), 'My custom service');
  await userEvent.type(
    screen.getByLabelText('Фактическая цена за период'),
    '12.34',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить подписку' }),
  );
  expect(
    await screen.findByRole('heading', { name: 'My custom service' }),
  ).toBeVisible();
  expect(writes[0]).toMatchObject({
    revision: 0,
    terms: {
      amount: '12.34',
      priceOrigin: 'USER_REPORTED',
      catalogVersion: null,
      additionalPerPeriod: null,
    },
  });
});
test('catalog prefill preserves evidence and manual override provenance', async () => {
  render(<App />);
  await screen.findByText(/Подписок пока нет/);
  await userEvent.click(
    screen.getByRole('button', { name: 'Добавить подписку' }),
  );
  await waitFor(() =>
    expect(
      screen.getByLabelText('Тариф из каталога').querySelectorAll('option')
        .length,
    ).toBeGreaterThan(1),
  );
  await userEvent.selectOptions(
    screen.getByLabelText('Тариф из каталога'),
    'creator-offer',
  );
  expect(screen.getByLabelText('Фактическая цена за период')).toHaveValue('15');
  await userEvent.clear(screen.getByLabelText('Фактическая цена за период'));
  await userEvent.type(
    screen.getByLabelText('Фактическая цена за период'),
    '7',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить подписку' }),
  );
  await waitFor(() => expect(writes).toHaveLength(1));
  expect(writes[0]).toMatchObject({
    terms: {
      priceOrigin: 'USER_REPORTED',
      amount: '7',
      offerId: 'creator-offer',
    },
  });
});
test('mustKeep updates current revision and deleting record does not promise provider cancellation', async () => {
  rows = [
    {
      id: 'one',
      terms: { ...emptySubscription(), name: 'Synthetic' },
      revision: 4,
      createdAt: '',
      updatedAt: '',
    },
  ];
  render(<App />);
  await screen.findByRole('heading', { name: 'Synthetic' });
  await userEvent.click(screen.getByLabelText('Обязательно сохранить'));
  await waitFor(() =>
    expect(writes[0]).toMatchObject({ revision: 4, terms: { mustKeep: true } }),
  );
  expect(
    screen.getByText(/Удаление записи в Compass не отменяет/),
  ).toBeVisible();
  await userEvent.click(screen.getByRole('button', { name: 'Удалить запись' }));
  expect(await screen.findByText(/Подписок пока нет/)).toBeVisible();
});
test('network failure has retry and preserves form after failed mutation', async () => {
  render(<App />);
  await screen.findByText(/Подписок пока нет/);
  await userEvent.click(
    screen.getByRole('button', { name: 'Добавить подписку' }),
  );
  await userEvent.type(screen.getByLabelText('Название'), 'Keep draft');
  failures = 500;
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить подписку' }),
  );
  expect(await screen.findByRole('alert')).toHaveTextContent('Не удалось');
  expect(screen.getByLabelText('Название')).toHaveValue('Keep draft');
  failures = 0;
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить подписку' }),
  );
  expect(
    await screen.findByRole('heading', { name: 'Keep draft' }),
  ).toBeVisible();
});
test('pending mustKeep responds immediately, locks comparison inputs and rolls back on failure', async () => {
  rows = [
    {
      id: 'one',
      terms: { ...emptySubscription(), name: 'Synthetic' },
      revision: 4,
      createdAt: '',
      updatedAt: '',
    },
  ];
  let release!: () => void;
  pendingWrite = new Promise<void>((resolve) => {
    release = resolve;
  });
  render(<App />);
  await screen.findByRole('heading', { name: 'Synthetic' });
  const toggle = screen.getByLabelText('Обязательно сохранить');
  await userEvent.click(toggle);
  expect(toggle).toBeChecked();
  expect(toggle).toBeDisabled();
  expect(screen.getByLabelText('Профиль потребностей')).toBeDisabled();
  expect(screen.getByLabelText('Что можно менять')).toBeDisabled();
  failures = 409;
  release();
  await screen.findByRole('alert');
  expect(toggle).not.toBeChecked();
  expect(toggle).toBeEnabled();
  expect(screen.getByLabelText('Что можно менять')).toBeEnabled();
});
test('comparison does not turn UNKNOWN savings into zero or multiply annual money in browser', () => {
  const scenario = {
    id: 'current',
    label: 'CURRENT',
    subscriptionIds: [],
    items: [],
    coverage: [],
    requirementsMet: false,
    lostFunctions: [],
    actions: [],
    potentialSavings: null,
    confirmedSavings: null,
    savingsStatus: 'UNKNOWN',
    conditions: [],
    costs: {
      paidAmount: '120',
      cashDueNow: '0',
      recurringMonthly: null,
      horizonTotal: null,
      mandatoryTotal: null,
      transitionCost: null,
      nextPayment: null,
      charges: [],
      unknowns: ['Missing price'],
    },
  };
  const report: OptimizationResult = {
    catalogVersion: 'synthetic',
    optimizerVersion: 'p07-1',
    profile: {
      profileSchemaVersion: 1,
      country: 'KZ',
      interfaceLanguage: 'ru',
      outputLanguages: [],
      needs: [],
      budget: {
        currency: 'USD',
        monthlyMax: '20',
        upfrontMax: '30',
        scope: 'ALL_AI',
      },
      allowAnnualBilling: false,
      maxTools: 4,
      horizonMonths: 12,
      priority: 'BALANCE',
      studentStatus: 'NONE',
      portfolioMode: 'REVIEW',
      purchaseChannel: 'WEB',
      requirements: {
        platforms: [],
        privacy: 'NO_STRICT_REQUIREMENT',
        features: [],
        usage: [],
      },
      existingSubscriptions: [],
      acceptedConditionEvidenceIds: [],
      asOf: null,
    },
    subscriptions: {},
    asOf: '2026-11-14T12:00:00Z',
    horizonMonths: 12,
    currency: 'USD',
    decision: 'UNKNOWN',
    current: scenario,
    alternatives: [{ ...scenario, id: 'alternative', label: 'OPTIMIZED' }],
    overlaps: [],
    recommendations: result(),
    fxSnapshots: [],
    limitations: [],
  };
  render(
    <MemoryRouter>
      <Comparison report={report} names={{}} />
    </MemoryRouter>,
  );
  expect(
    screen.getByRole('heading', { name: 'Экономию пока нельзя определить' }),
  ).toBeVisible();
  expect(screen.getAllByText(/UNKNOWN/).length).toBeGreaterThan(1);
  expect(screen.getAllByText('120 USD')).toHaveLength(2);
  expect(screen.queryByText('1440 USD')).not.toBeInTheDocument();
});
