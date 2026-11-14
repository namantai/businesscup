import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, afterEach, expect, test, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';
import { NoticeCard } from './InboxPage';
import { result } from '../builder/test-data';
import type { Account, Notice } from '../api/generated/types.gen';

let role: string;
let paths: string[];
let failure: boolean;
const response = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
beforeEach(() => {
  role = 'USER';
  paths = [];
  failure = false;
  history.replaceState({}, '', '/notifications');
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request | string) => {
      const r = typeof input === 'string' ? new Request(input) : input;
      const path = new URL(r.url).pathname;
      paths.push(path);
      if (path.endsWith('/auth/session'))
        return response({
          account: {
            id: 'test-user',
            role,
            email: 'synthetic@example.invalid',
            verified: true,
            plan: 'FREE',
            createdAt: '2026-11-15T00:00:00Z',
          } as Account,
        });
      if (path.endsWith('/auth/csrf'))
        return response({
          token: 'synthetic-csrf',
          headerName: 'X-CSRF-TOKEN',
        });
      if (failure) return response({}, 500);
      if (path.endsWith('/active'))
        return response({ versionId: null, revision: 0 });
      return response([]);
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
test('USER cannot load admin drafts or audit', async () => {
  history.replaceState({}, '', '/admin/catalog');
  render(<App />);
  expect(await screen.findByText(/Требуется роль ADMIN/)).toBeVisible();
  expect(paths.some((p) => p.includes('/admin/'))).toBe(false);
});
test('ADMIN sees empty draft queue and publication disabled without review', async () => {
  role = 'ADMIN';
  history.replaceState({}, '', '/admin/catalog');
  render(<App />);
  expect(await screen.findByText('Черновиков пока нет.')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Опубликовать' })).toBeDisabled();
  expect(screen.getByLabelText('Manifest JSON')).toBeVisible();
});
test('inbox explains local outbox and honest empty state', async () => {
  render(<App />);
  expect(
    await screen.findByText('Существенных изменений пока нет.'),
  ).toBeVisible();
  expect(screen.getByLabelText(/Сохранять копию/)).not.toBeChecked();
  expect(screen.getByLabelText(/Согласен на повторный/)).not.toBeChecked();
});
test('inbox recovers from server failure', async () => {
  failure = true;
  render(<App />);
  expect(await screen.findByRole('alert')).toBeVisible();
  failure = false;
  await userEvent.click(
    screen.getByRole('button', { name: 'Обновить уведомления' }),
  );
  await waitFor(() =>
    expect(screen.getByText('Существенных изменений пока нет.')).toBeVisible(),
  );
});
test('notice keeps UNKNOWN and self reported action separate from savings', () => {
  const old = result();
  old.bundles[0].costs.horizonTotal = null;
  const notice: Notice = {
    id: 'notice',
    watchId: 'watch',
    publicationRevision: 2,
    read: false,
    appliedSelfReported: true,
    createdAt: '2026-11-15T00:00:00Z',
    horizonMonths: 12,
    comparison: {
      previousVersion: 'old',
      currentVersion: 'new',
      changes: [{ field: 'offer/amount', before: '20', after: 'UNKNOWN' }],
      before: old,
      after: old,
      explanation: 'Недостаточно данных',
    },
  };
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <NoticeCard
        notice={notice}
        busy={false}
        onApply={vi.fn()}
        onRead={vi.fn()}
      />
    </QueryClientProvider>,
  );
  expect(screen.getByText(/покупка и экономия не подтверждены/)).toBeVisible();
  expect(screen.getAllByText(/за 12 мес.: UNKNOWN/)).toHaveLength(2);
  expect(screen.getAllByText(/DEMO: синтетические данные/)).toHaveLength(2);
  expect(
    screen.getByRole('button', { name: /Отметить применение/ }),
  ).toBeDisabled();
});
