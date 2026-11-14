import { beforeEach, afterEach, expect, test, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';
let enabled = false;
let mutations: string[] = [];
const response = (data: unknown) =>
  new Response(JSON.stringify(data), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
beforeEach(() => {
  enabled = false;
  mutations = [];
  history.replaceState({}, '', '/account');
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request | string) => {
      const request = typeof input === 'string' ? new Request(input) : input;
      const path = new URL(request.url).pathname;
      if (path.endsWith('/auth/session'))
        return response({
          account: {
            id: 'synthetic',
            email: 'synthetic@example.invalid',
            role: 'USER',
            verified: true,
            plan: 'FREE',
            createdAt: '2026-11-15T00:00:00Z',
          },
        });
      if (path.endsWith('/auth/csrf'))
        return response({
          headerName: 'X-CSRF-TOKEN',
          token: 'synthetic-csrf',
        });
      if (request.method === 'PATCH') {
        expect(request.headers.get('X-CSRF-TOKEN')).toBe('synthetic-csrf');
        const body = await request.json();
        expect(Object.keys(body).sort()).toEqual(['confirm', 'enabled']);
        expect(body.confirm).toBe(true);
        enabled = body.enabled;
        mutations.push(path);
      }
      if (request.method === 'POST') mutations.push(path);
      return response({ enabled, retentionDays: 30, events: [] });
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
test('analytics is off until an explicit opt-in and save', async () => {
  render(<App />);
  const box = await screen.findByLabelText(
    'Согласен на локальную аналитику указанных событий',
  );
  await waitFor(() => expect(box).toBeEnabled());
  expect(box).not.toBeChecked();
  expect(mutations).toHaveLength(0);
  await userEvent.click(box);
  expect(mutations).toHaveLength(0);
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить настройку аналитики' }),
  );
  await waitFor(() =>
    expect(mutations).toContain('/api/v1/me/analytics/revisit'),
  );
  expect(mutations.filter((p) => p.endsWith('/revisit'))).toHaveLength(1);
});
test('withdrawing analytics updates the visible state without payment metrics', async () => {
  enabled = true;
  render(<App />);
  const box = await screen.findByLabelText(
    'Согласен на локальную аналитику указанных событий',
  );
  await waitFor(() => expect(box).toBeChecked());
  await userEvent.click(box);
  await userEvent.click(
    screen.getByRole('button', { name: 'Сохранить настройку аналитики' }),
  );
  expect(await screen.findByText(/Сбор выключен/)).toBeVisible();
  expect(mutations.some((p) => p.includes('payment'))).toBe(false);
});
