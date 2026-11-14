import { beforeEach, afterEach, expect, test, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';
import { MemoryRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BuilderSession, useBuilder } from '../builder/BuilderSession';
import { AuthSession, useAuth } from './AuthSession';
import { SavePanel } from './SavePanel';
import { catalog, catalogResponse, result } from '../builder/test-data';
import { emptyDraft, toProfile } from '../builder/draft';
import type { Account, SavedProfile } from '../api/generated/types.gen';
const userAccount: Account = {
  id: 'user-a',
  email: 'synthetic@example.invalid',
  role: 'USER',
  verified: true,
  plan: 'FREE',
  createdAt: '2026-11-15T00:00:00Z',
};
const password = 'Synthetic-password-only';
const response = (data: unknown, status = 200) =>
  new Response(status === 204 ? null : JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
let account: Account | null;
let savedProfiles: SavedProfile[];
let requests: Array<{ path: string; body: Record<string, unknown> }>;
let status: number;
let csrfCount: number;
function profile() {
  return toProfile({
    ...emptyDraft(),
    country: 'KZ',
    interfaceLanguage: 'ru',
    purchaseChannel: 'WEB',
    needs: [{ id: 'study', weight: 3, required: true, intensity: 'WEEKLY' }],
    budget: {
      currency: 'USD',
      monthlyMax: '20',
      upfrontMax: '30',
      scope: 'ALL_AI',
    },
  });
}
beforeEach(() => {
  history.replaceState({}, '', '/login');
  account = null;
  savedProfiles = [];
  requests = [];
  status = 200;
  csrfCount = 0;
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request | string) => {
      const req = typeof input === 'string' ? new Request(input) : input;
      const path = new URL(req.url).pathname;
      if (path.endsWith('/auth/session')) return response({ account });
      if (path.endsWith('/auth/csrf')) {
        csrfCount++;
        return response({
          headerName: 'X-CSRF-TOKEN',
          token: 'synthetic-' + csrfCount,
        });
      }
      if (req.method !== 'GET') {
        const text = await req.clone().text();
        const body = text ? JSON.parse(text) : {};
        requests.push({ path, body });
        expect(req.headers.get('X-CSRF-TOKEN')).toBe('synthetic-' + csrfCount);
        if (status !== 200) return response({ status }, status);
        if (path.endsWith('/auth/login')) {
          account = userAccount;
          return response(account);
        }
        if (path.endsWith('/auth/logout')) {
          account = null;
          return response(null, 204);
        }
        if (path.endsWith('/me')) {
          account = null;
          return response(null, 204);
        }
        if (path.endsWith('/recommendations'))
          return response(
            {
              id: 'saved-1',
              profile: profile(),
              result: result(),
              savedAt: '2026-11-15T00:00:00Z',
            },
            201,
          );
        if (path.endsWith('/saved-stacks'))
          return response(
            { id: 'stack-1', ...body, createdAt: '2026-11-15T00:00:00Z' },
            201,
          );
        return response(
          { message: 'Synthetic response' },
          path.endsWith('/register') ? 202 : 200,
        );
      }
      if (path.endsWith('/profiles')) return response(savedProfiles);
      if (path.endsWith('/recommendations') || path.endsWith('/saved-stacks'))
        return response([]);
      if (path.endsWith('/me/export'))
        return response({
          account,
          profiles: savedProfiles,
          recommendations: [],
          stacks: [],
        });
      return response(catalogResponse(new URL(req.url)));
    }),
  );
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
async function fillCredentials() {
  const u = userEvent.setup();
  await u.type(screen.getByLabelText('Email'), userAccount.email);
  await u.type(screen.getByLabelText(/^Пароль/), password);
  return u;
}
test('registration is explicit, uses CSRF and never silently saves the guest draft', async () => {
  history.replaceState({}, '', '/register');
  render(<App />);
  const u = await fillCredentials();
  await u.click(screen.getByRole('button', { name: 'Создать аккаунт' }));
  await screen.findByText(/Если адрес подходит, письмо доступно/);
  expect(requests.map((r) => r.path)).toEqual(['/api/v1/auth/register']);
  expect(csrfCount).toBe(1);
  expect(screen.getByLabelText(/^Пароль/)).toHaveValue('');
});
test('login uses only credentials and opens the empty personal dashboard', async () => {
  render(<App />);
  const u = await fillCredentials();
  await u.click(screen.getByRole('button', { name: 'Войти' }));
  await screen.findByText(/Пока нет профиля/);
  expect(requests[0].body).toEqual({ email: userAccount.email, password });
  expect(screen.getByText(/Сохранённых результатов пока нет/)).toBeVisible();
});
test.each([401, 403, 429, 500])(
  'auth error %s preserves email, clears password and allows a new CSRF attempt',
  async (code) => {
    status = code;
    render(<App />);
    const u = await fillCredentials();
    await u.click(screen.getByRole('button', { name: 'Войти' }));
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.getByLabelText('Email')).toHaveValue(userAccount.email);
    expect(screen.getByLabelText(/^Пароль/)).toHaveValue('');
    status = 200;
    await u.type(screen.getByLabelText(/^Пароль/), password);
    await u.click(screen.getByRole('button', { name: 'Войти' }));
    await screen.findByText(/Пока нет профиля/);
    expect(csrfCount).toBe(2);
  },
);
test('verification token is removed from URL and never rendered as ordinary text', async () => {
  const token = 'T'.repeat(43);
  history.replaceState({}, '', '/verify-email#token=' + token);
  render(<App />);
  expect(location.hash).toBe('');
  expect(screen.getByLabelText('Код из письма')).toHaveAttribute(
    'type',
    'password',
  );
  expect(screen.queryByText(token)).not.toBeInTheDocument();
  await userEvent.click(
    screen.getByRole('button', { name: 'Подтвердить email' }),
  );
  await screen.findByText('Email подтверждён. Теперь можно войти.');
  expect(requests[0].body).toEqual({ token });
});
test('account deletion needs explicit confirmation and password; logout leaves no personal page', async () => {
  account = userAccount;
  history.replaceState({}, '', '/account');
  render(<App />);
  await screen.findByText(userAccount.email);
  const button = screen.getByRole('button', {
    name: 'Удалить аккаунт безвозвратно',
  });
  expect(button).toBeDisabled();
  const u = userEvent.setup();
  await u.type(screen.getByLabelText('Текущий пароль'), password);
  await u.click(
    screen.getByLabelText('Подтверждаю удаление аккаунта и личных данных'),
  );
  await u.click(button);
  await screen.findByRole('heading', { name: /Ваш набор AI/ });
  expect(requests[0].body).toEqual({ password, confirm: true });
});
test('saved profile opens the real builder without auto-submitting or automatic save', async () => {
  account = userAccount;
  savedProfiles = [
    {
      id: 'profile-a',
      name: 'My needs',
      profile: profile(),
      revision: 1,
      createdAt: '2026-11-15T00:00:00Z',
      updatedAt: '2026-11-15T00:00:00Z',
    },
  ];
  history.replaceState({}, '', '/dashboard');
  render(<App />);
  await userEvent.click(
    await screen.findByRole('button', {
      name: 'Открыть профиль и повторить подбор',
    }),
  );
  await screen.findByLabelText('Synthetic study');
  expect(screen.getByLabelText('Synthetic study')).toBeChecked();
  expect(requests).toHaveLength(0);
});
function FixtureControls() {
  const b = useBuilder();
  const auth = useAuth();
  return (
    <>
      <button
        onClick={() =>
          b.restore({ profile: profile(), response: result(), catalog })
        }
      >
        Load fixture
      </button>
      <button onClick={() => auth.accept({ ...userAccount, id: 'user-b' })}>
        Switch account
      </button>
      <SavePanel />
    </>
  );
}
function fixture() {
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <MemoryRouter initialEntries={['/results']}>
        <BuilderSession>
          <AuthSession>
            <FixtureControls />
          </AuthSession>
        </BuilderSession>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
test('saving requires consent and sends reproducible inputs instead of client ranking or money', async () => {
  account = userAccount;
  fixture();
  const u = userEvent.setup();
  await u.click(screen.getByRole('button', { name: 'Load fixture' }));
  const button = await screen.findByRole('button', {
    name: 'Сохранить результат',
  });
  expect(button).toBeDisabled();
  expect(requests).toHaveLength(0);
  await u.click(
    screen.getByLabelText(
      'Подтверждаю сохранение указанных параметров в этот аккаунт',
    ),
  );
  await u.click(button);
  await screen.findByText('Исторический результат сохранён.');
  expect(Object.keys(requests[0].body).sort()).toEqual([
    'asOf',
    'catalogVersion',
    'engineVersion',
    'policyVersion',
    'profile',
  ]);
  expect(requests[0].body.profile).toEqual(profile());
});
test('switching account clears the previous account draft and result', async () => {
  account = userAccount;
  fixture();
  await userEvent.click(screen.getByRole('button', { name: 'Load fixture' }));
  await screen.findByText(/Аккаунт: synthetic/);
  await userEvent.click(screen.getByRole('button', { name: 'Switch account' }));
  await waitFor(() =>
    expect(screen.queryByText('Сохранить в аккаунт')).not.toBeInTheDocument(),
  );
});
