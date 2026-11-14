import { beforeEach, afterEach, expect, test, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';
let failure = false,
  pending = false,
  premium = false;
const response = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
beforeEach(() => {
  failure = false;
  pending = false;
  premium = false;
  history.replaceState({}, '', '/pricing');
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
            plan: premium ? 'PREMIUM' : 'FREE',
            createdAt: '2026-11-15T00:00:00Z',
          },
        });
      if (pending) return new Promise<Response>(() => {});
      if (failure) return response({}, 500);
      if (path.endsWith('/billing/plans'))
        return response([
          {
            name: 'FREE',
            profileLimit: 1,
            watchLimit: 0,
            history: false,
            feeAmount: '0.00',
            currency: 'USD',
            availability: 'Доступен бесплатно',
          },
          {
            name: 'PREMIUM',
            profileLimit: 5,
            watchLimit: 5,
            history: true,
            feeAmount: '0.00',
            currency: 'USD',
            availability: 'Beta: оплата недоступна',
          },
        ]);
      if (path.endsWith('/audit')) return response([]);
      if (path.endsWith('/entitlements'))
        return response({
          plan: premium ? 'PREMIUM' : 'FREE',
          profileLimit: premium ? 5 : 1,
          watchLimit: premium ? 5 : 0,
          history: premium,
          expiresAt: premium ? '2026-12-01T00:00:00Z' : null,
          revision: premium ? 1 : 0,
          grantState: premium ? 'ACTIVE' : 'NONE',
          paymentMode: 'OFF',
          feeAmount: '0.00',
          feeCurrency: 'USD',
        });
      return response([]);
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
test('plans show honest beta and no checkout', async () => {
  render(<App />);
  expect(await screen.findByRole('heading', { name: 'PREMIUM' })).toBeVisible();
  expect(screen.getByText(/Нет автопродления/)).toBeVisible();
  expect(
    screen.queryByRole('button', { name: /Оплатить/ }),
  ).not.toBeInTheDocument();
  expect(screen.getByText('Текущий сохранённый результат')).toBeVisible();
});
test('plans show loading', async () => {
  pending = true;
  render(<App />);
  expect(await screen.findByText('Загружаем условия…')).toBeVisible();
});
test('plans error is recoverable', async () => {
  failure = true;
  render(<App />);
  expect(await screen.findByRole('alert')).toBeVisible();
  failure = false;
  await userEvent.click(screen.getByRole('button', { name: 'Повторить' }));
  expect(await screen.findByRole('heading', { name: 'FREE' })).toBeVisible();
});
test('account shows free empty grant history and no admin issuer', async () => {
  history.replaceState({}, '', '/account/billing');
  render(<App />);
  expect(
    await screen.findByText('Beta-доступ ещё не выдавался.'),
  ).toBeVisible();
  expect(screen.queryByLabelText('UUID аккаунта')).not.toBeInTheDocument();
  expect(screen.getByText('Состояние beta: не выдавался')).toBeVisible();
});
test('active beta shows expiration and zero actual fee', async () => {
  premium = true;
  history.replaceState({}, '', '/account/billing');
  render(<App />);
  expect(
    await screen.findByText(/Дата окончания выданного доступа/),
  ).toBeVisible();
  expect(screen.getByText(/Сбор Compass: 0.00 USD/)).toBeVisible();
});
