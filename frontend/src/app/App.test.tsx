import { beforeEach, afterEach, expect, test, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from './App';

// The original health/status page is preserved at /about.
beforeEach(() => window.history.replaceState({}, '', '/about'));
afterEach(() => {
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', '/');
});

test('shows pending state without claiming availability', () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(() => new Promise(() => {})),
  );
  render(<App />);
  expect(screen.getByRole('status')).toHaveTextContent('Проверяем подключение');
  expect(screen.getByRole('button')).toBeDisabled();
});

test('shows availability only after an UP response', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify({ status: 'UP' }))),
  );
  render(<App />);
  expect(await screen.findByText('Сервис доступен')).toBeVisible();
  expect(screen.getByText(/Без ввода конкретного задания/)).toBeVisible();
});

test('shows a failed request and can retry successfully', async () => {
  let healthCalls = 0;
  const fetchMock = vi.fn((request: Request | string) => {
    if (request === '/actuator/health/readiness') {
      if (++healthCalls === 1) return Promise.reject(new Error('network'));
      return Promise.resolve(new Response(JSON.stringify({ status: 'UP' })));
    }
    // AuthSession also refreshes when visiting the relocated /about route.
    return Promise.resolve(new Response(JSON.stringify({ account: null })));
  });
  vi.stubGlobal('fetch', fetchMock);
  render(<App />);
  expect(
    await screen.findByText('Не удалось подключиться к сервису'),
  ).toBeVisible();
  await userEvent.click(screen.getByRole('button'));
  expect(await screen.findByText('Сервис доступен')).toBeVisible();
  expect(healthCalls).toBe(2);
});

test.each([{ status: 'DOWN' }, {}, { status: 200 }])(
  'rejects an unhealthy or invalid response: %j',
  async (body) => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify(body))),
    );
    render(<App />);
    expect(
      await screen.findByText('Не удалось подключиться к сервису'),
    ).toBeVisible();
  },
);

test('rejects a failed HTTP response even if its body says UP', async () => {
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify({ status: 'UP' }), { status: 503 }),
      ),
  );
  render(<App />);
  expect(
    await screen.findByText('Не удалось подключиться к сервису'),
  ).toBeVisible();
});
