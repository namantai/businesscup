import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App } from '../app/App';
import { catalog, catalogResponse, result } from './test-data';
import { emptyDraft, toProfile, validateDraft } from './draft';
import type {
  RecommendationProfile,
  RecommendationResult,
} from '../api/generated/types.gen';
import { PreviewError, requestPreview } from '../api/builder';
const json = (body: unknown, status = 200, headers = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });
let sent: RecommendationProfile[];
let csrfCalls: number;
let answer: () => Promise<Response>;
let requests: Request[];
beforeEach(() => {
  history.replaceState({}, '', '/onboarding');
  sent = [];
  requests = [];
  csrfCalls = 0;
  answer = async () => json(result());
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request | string) => {
      const request = typeof input === 'string' ? new Request(input) : input;
      requests.push(request);
      const url = new URL(request.url);
      if (url.pathname.endsWith('/csrf')) {
        csrfCalls++;
        return json({
          token: 'synthetic-session-token',
          headerName: 'X-CSRF-TOKEN',
        });
      }
      if (url.pathname.endsWith('/preview')) {
        sent.push(await request.clone().json());
        expect(request.headers.get('X-CSRF-TOKEN')).toBe(
          'synthetic-session-token',
        );
        return answer();
      }
      return json(catalogResponse(url));
    }),
  );
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  history.replaceState({}, '', '/');
});
const user = () => userEvent.setup();
async function start() {
  render(<App />);
  await user().click(
    await screen.findByRole('button', { name: /Настроить самому/ }),
  );
  await screen.findByLabelText('Synthetic study');
}
async function need(name = 'Synthetic study') {
  const u = user();
  await u.click(screen.getByLabelText(name));
  await u.click(
    within(screen.getByLabelText(name).closest('div')!).getByText(
      'Настроить цель',
    ),
  );
  await u.selectOptions(screen.getByLabelText(`Частота: ${name}`), 'WEEKLY');
}
async function next() {
  await user().click(screen.getByRole('button', { name: 'Продолжить' }));
}
async function finish(budget = '20') {
  await next();
  const u = user();
  await u.type(screen.getByLabelText('Страна покупки (код)'), 'KZ');
  await u.click(screen.getByText('Дополнительные настройки'));
  await u.type(screen.getByLabelText('Язык интерфейса (код)'), 'ru');
  await u.type(screen.getByLabelText('Способ покупки'), 'WEB');
  await u.type(screen.getByLabelText('Валюта бюджета'), 'USD');
  await u.type(screen.getByLabelText('Месячный бюджет на весь AI'), budget);
  await u.type(screen.getByLabelText('Доступная предоплата'), '0');
}
async function submit() {
  await user().click(
    screen.getByRole('button', { name: 'Подобрать AI-набор' }),
  );
}

test('multi-needs, weights, optional flags, budget and CSRF are sent through the generated SDK', async () => {
  await start();
  await need();
  await need('Synthetic coding');
  await user().selectOptions(
    screen.getByLabelText('Приоритет: Synthetic coding'),
    '5',
  );
  await user().click(screen.getByLabelText('Обязательно: Synthetic study'));
  await finish('0');
  await submit();
  await screen.findByRole('heading', { name: 'Результаты подбора' });
  expect(sent).toHaveLength(1);
  expect(csrfCalls).toBe(1);
  expect(sent[0].needs).toEqual([
    { id: 'study', weight: 3, required: false, intensity: 'WEEKLY' },
    { id: 'coding', weight: 5, required: true, intensity: 'WEEKLY' },
  ]);
  expect(sent[0].budget).toEqual({
    currency: 'USD',
    monthlyMax: '0',
    upfrontMax: '0',
    scope: 'ALL_AI',
  });
  expect(sent[0].asOf).toBeNull();
  expect(sent[0]).not.toHaveProperty('missingNeed');
  expect(requests.every((r) => r.credentials === 'same-origin')).toBe(true);
});
test('validation focuses the error summary, does not invent missing values or a missing need', async () => {
  await start();
  await next();
  expect(screen.getByRole('alert')).toHaveFocus();
  expect(screen.getByRole('alert')).toHaveTextContent('Выберите от одной');
  await need();
  await user().click(
    screen.getByLabelText(
      'В каталоге нет одной из моих обязательных потребностей',
    ),
  );
  await next();
  expect(screen.getByRole('alert')).toHaveTextContent(
    'недостаточно подтверждённых данных',
  );
  expect(sent).toHaveLength(0);
});
test('existing subscription, unknown amount and mustKeep remain explicit, draft survives internal navigation', async () => {
  const persistent = vi.spyOn(Storage.prototype, 'setItem');
  await start();
  await need();
  await finish();
  const u = user();
  await u.click(
    screen.getByRole('button', { name: 'Добавить текущую подписку' }),
  );
  await u.selectOptions(
    screen.getByLabelText('Тариф и предложение'),
    'learner-offer',
  );
  await u.type(screen.getByLabelText('Валюта платежа'), 'USD');
  await u.click(screen.getByLabelText('Цена неизвестна'));
  await u.selectOptions(screen.getByLabelText('Период оплаты'), 'MONTH');
  await u.type(screen.getByLabelText('Исходная дата продления'), '2026-11-14');
  await u.type(screen.getByLabelText('Следующее продление'), '2026-12-14');
  await u.type(screen.getByLabelText('Часовой пояс платежа'), 'Asia/Almaty');
  await u.click(screen.getByLabelText('Обязательно сохранить (mustKeep)'));
  await u.click(screen.getByText('Ещё'));
  await u.click(screen.getByRole('link', { name: 'Как это работает' }));
  await u.click(screen.getByRole('link', { name: 'Подбор' }));
  await u.click(await screen.findByText('Дополнительные настройки'));
  expect(
    await screen.findByLabelText('Обязательно сохранить (mustKeep)'),
  ).toBeChecked();
  await submit();
  await screen.findByRole('heading', { name: 'Результаты подбора' });
  expect(sent[0].existingSubscriptions[0]).toMatchObject({
    planId: 'learner-plan',
    offerId: 'learner-offer',
    amount: null,
    mustKeep: true,
  });
  expect(persistent).not.toHaveBeenCalled();
});
test.each([400, 403, 413, 429, 500, 0])(
  'handles %s and preserves draft for retry',
  async (status) => {
    answer = async () => {
      if (!status) throw new TypeError('Offline');
      return json(
        { status },
        status,
        status === 429 ? { 'Retry-After': '0' } : {},
      );
    };
    await start();
    await need();
    await finish();
    await submit();
    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.getByLabelText('Месячный бюджет на весь AI')).toHaveValue(
      '20',
    );
    expect(screen.queryByTestId('bundle-card')).not.toBeInTheDocument();
    answer = async () => json(result());
    await submit();
    await screen.findByRole('heading', { name: 'Результаты подбора' });
    expect(csrfCalls).toBe(2);
  },
);
test('429 honours Retry-After and disables repeated sends', async () => {
  answer = async () => json({}, 429, { 'Retry-After': '60' });
  await start();
  await need();
  await finish();
  await submit();
  await screen.findByText(/Повторный запрос доступен через/);
  expect(
    screen.getByRole('button', { name: 'Подобрать AI-набор' }),
  ).toBeDisabled();
  expect(sent).toHaveLength(1);
});
test('loading prevents duplicate submissions and cancelled responses cannot replace the current page', async () => {
  let resolve!: (r: Response) => void;
  answer = () =>
    new Promise((r) => {
      resolve = r;
    });
  await start();
  await need();
  await finish();
  await submit();
  await screen.findByText('Подбираем набор по опубликованному каталогу…');
  expect(
    screen.getByRole('button', { name: 'Подобрать AI-набор' }),
  ).toBeDisabled();
  await user().click(screen.getByRole('button', { name: 'Отменить запрос' }));
  resolve(json(result()));
  await waitFor(() =>
    expect(
      screen.queryByText('Подбираем набор по опубликованному каталогу…'),
    ).not.toBeInTheDocument(),
  );
  expect(screen.queryByTestId('bundle-card')).not.toBeInTheDocument();
  expect(location.pathname).toBe('/onboarding');
  expect(sent).toHaveLength(1);
});
test('publication changes cannot mix old catalog names with a new result', async () => {
  answer = async () => json({ ...result(), catalogVersion: 'new-snapshot' });
  await start();
  await need();
  await finish();
  await submit();
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Каталог изменился',
  );
  expect(screen.queryByTestId('bundle-card')).not.toBeInTheDocument();
});
test.each([1, 2, 3])(
  'renders exactly %s backend bundles, sources and pinned product links',
  async (count) => {
    answer = async () => json(result(count));
    await start();
    await need();
    await finish();
    await submit();
    expect(await screen.findAllByTestId('bundle-card')).toHaveLength(count);
    const card = screen.getAllByTestId('bundle-card')[0];
    expect(within(card).getByText(/FREE ·/)).toBeVisible();
    const link = within(card).getByRole('link', { name: 'Synthetic learner' });
    expect(link).toHaveAttribute(
      'href',
      `/catalog/learner?version=${catalog.version}`,
    );
    await user().click(within(card).getByText(/Источники и даты проверки/));
    expect(
      within(card)
        .getAllByRole('link')
        .some((a) => a.getAttribute('href')?.startsWith('https://')),
    ).toBe(true);
  },
);
test('no-match, unknown, conditional, costs and annual horizon are not fabricated', async () => {
  const response = result(0);
  const b = result().bundles[0];
  b.eligibility = 'CONDITIONAL';
  b.strategies = [];
  b.costs = {
    ...b.costs,
    cashDueNow: null,
    recurringMonthly: null,
    horizonTotal: null,
    upfront: null,
    confirmedBudgetFit: false,
    confidence: 'INCOMPLETE',
  };
  b.conditions = ['Synthetic student verification required'];
  response.conditionalAlternatives = [b];
  response.unmetRequirements = [
    { capabilityId: 'video', state: 'UNKNOWN', reason: 'UNKNOWN_CAPABILITY' },
  ];
  response.warnings = [
    {
      code: 'PRICE_INCOMPLETE',
      detail: 'Synthetic unknown taxes',
      evidenceIds: [],
    },
  ];
  answer = async () => json(response);
  await start();
  await need();
  await finish();
  await submit();
  await screen.findByRole('heading', {
    name: 'Пока не нашли подходящий вариант.',
  });
  expect(screen.getAllByText('Цена не подтверждена').length).toBeGreaterThan(2);
  expect(screen.queryByText(/FREE ·/)).not.toBeInTheDocument();
  expect(
    screen.getByRole('region', { name: 'Условные варианты' }),
  ).toBeVisible();
  expect(screen.getByText('Непокрытые требования')).toBeVisible();
  expect(
    screen.getByRole('heading', {
      name: 'Требует проверки: Synthetic learner / Synthetic learner',
    }),
  ).toBeVisible();
});
test('direct results route has no invented cards and editing requires a fresh request', async () => {
  history.replaceState({}, '', '/results');
  render(<App />);
  expect(screen.getByText('Сначала расскажите о потребностях')).toBeVisible();
  expect(screen.queryByTestId('bundle-card')).not.toBeInTheDocument();
});
test('form validators retain UNKNOWN and normalize decimal text without floating-point arithmetic', () => {
  const d = emptyDraft();
  expect(validateDraft(d, catalog).length).toBeGreaterThan(4);
  d.needs = [{ id: 'study', weight: 3, required: true, intensity: 'WEEKLY' }];
  d.budget = {
    currency: 'USD',
    monthlyMax: '120,000001',
    upfrontMax: '0',
    scope: 'ALL_AI',
  };
  expect(toProfile(d).budget.monthlyMax).toBe('120.000001');
  d.budget.monthlyMax = '-1';
  expect(validateDraft(d, catalog).some((e) => e.field === 'monthlyMax')).toBe(
    true,
  );
});
test('failed CSRF never sends the profile', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => json({}, 403)),
  );
  await expect(
    requestPreview(
      toProfile({
        ...emptyDraft(),
        needs: [
          { id: 'study', weight: 3, required: true, intensity: 'WEEKLY' },
        ],
      }),
      new AbortController().signal,
    ),
  ).rejects.toBeInstanceOf(PreviewError);
  expect(fetch).toHaveBeenCalledTimes(1);
});
// Compile-time contract check: fixtures and component adapters use generated DTOs.
const typed: RecommendationResult = result();
void typed;

test('features, platform, language, privacy and annual upfront are explicit profile requirements', async () => {
  await start();
  await need();
  await next();
  const u = user();
  await u.type(screen.getByLabelText('Страна покупки (код)'), 'KZ');
  await u.click(screen.getByText('Дополнительные настройки'));
  await u.type(screen.getByLabelText('Язык интерфейса (код)'), 'ru');
  await u.type(
    screen.getByLabelText('Языки ответов (необязательно)'),
    'ru, en',
  );
  await u.type(screen.getByLabelText('Способ покупки'), 'WEB');
  await u.click(screen.getByLabelText('Linux'));
  await u.selectOptions(
    screen.getByLabelText('Конфиденциальность'),
    'LOCAL_ONLY',
  );
  const feature = catalog.capabilities.find((c) => c.category === 'FUNCTION')!;
  await u.click(screen.getByLabelText(feature.name, { exact: true }));
  await u.click(screen.getByLabelText(`Обязательна: ${feature.name}`));
  await u.selectOptions(
    screen.getByLabelText(`Важность: ${feature.name}`),
    '5',
  );
  await u.type(screen.getByLabelText('Валюта бюджета'), 'USD');
  await u.type(screen.getByLabelText('Месячный бюджет на весь AI'), '10,25');
  await u.type(screen.getByLabelText('Доступная предоплата'), '123');
  await u.click(
    screen.getByLabelText('Разрешить годовую оплату в пределах предоплаты'),
  );
  await submit();
  await screen.findByRole('heading', { name: 'Результаты подбора' });
  expect(sent[0]).toMatchObject({
    allowAnnualBilling: true,
    outputLanguages: ['ru', 'en'],
    budget: { monthlyMax: '10.25', upfrontMax: '123' },
    requirements: {
      platforms: ['LINUX'],
      privacy: 'LOCAL_ONLY',
      features: [{ capabilityId: feature.id, required: false, weight: 5 }],
    },
  });
});
test('a late cancelled answer cannot overwrite a newer completed preview', async () => {
  let resolve!: (r: Response) => void;
  answer = () =>
    new Promise((r) => {
      resolve = r;
    });
  await start();
  await need();
  await finish();
  await submit();
  await screen.findByText('Подбираем набор по опубликованному каталогу…');
  await user().click(screen.getByRole('button', { name: 'Отменить запрос' }));
  answer = async () => json({ ...result(), requestId: 'new-preview' });
  await submit();
  await screen.findByRole('heading', { name: 'Результаты подбора' });
  resolve(json({ ...result(3), requestId: 'old-preview' }));
  await waitFor(() =>
    expect(screen.getAllByTestId('bundle-card')).toHaveLength(1),
  );
  await user().click(screen.getByText('Данные расчёта'));
  expect(screen.getByText(/new-preview/)).toBeVisible();
  expect(screen.queryByText(/old-preview/)).not.toBeInTheDocument();
});
test('an empty catalog never enables fake recommendations', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      json({
        ...catalogResponse(new URL('http://localhost/api/v1/catalog/meta')),
        versionId: null,
        status: 'EMPTY',
      }),
    ),
  );
  await startEmpty();
  expect(
    screen.queryByRole('button', { name: 'Подобрать AI-набор' }),
  ).not.toBeInTheDocument();
});
async function startEmpty() {
  render(<App />);
  await screen.findByText(/Каталог пока не опубликован/);
}

test('reset clears the whole local draft and result without browser persistence', async () => {
  await start();
  await need();
  await finish();
  await user().click(screen.getByRole('button', { name: 'Очистить анкету' }));
  await user().click(screen.getByRole('button', { name: /Настроить самому/ }));
  expect(screen.getByLabelText('Synthetic study')).not.toBeChecked();
  await user().click(
    screen.getByRole('button', { name: '3. Бюджет и предпочтения' }),
  );
  expect(screen.getByLabelText('Месячный бюджет на весь AI')).toHaveValue('');
  await user().click(screen.getByText('Ещё'));
  await user().click(screen.getByRole('link', { name: 'Результаты' }));
  expect(screen.getByText('Сначала расскажите о потребностях')).toBeVisible();
});

test('continue does not submit the newly mounted budget step; corrected fields clear stale errors', async () => {
  await start();
  await need();
  await finish();
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(sent).toHaveLength(0);
  await user().clear(screen.getByLabelText('Месячный бюджет на весь AI'));
  await submit();
  expect(screen.getByRole('alert')).toBeVisible();
  await user().type(screen.getByLabelText('Месячный бюджет на весь AI'), '20');
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Месячный бюджет на весь AI')).toHaveFocus();
  expect(screen.getByLabelText('Месячный бюджет на весь AI')).toHaveValue('20');
});

test('role presets are editable and do not imply student eligibility or missing values', async () => {
  render(<App />);
  await user().click(
    await screen.findByRole('button', { name: /Студент-разработчик/ }),
  );
  expect(screen.getByLabelText('Synthetic study')).toBeChecked();
  expect(screen.getByLabelText('Synthetic coding')).not.toBeChecked();
  // Fixture uses coding, not development: the preset must not invent a mapping.
  await need('Synthetic coding');
  await next();
  expect(screen.getByLabelText('Страна покупки (код)')).toHaveValue('');
  expect(screen.getByLabelText('Месячный бюджет на весь AI')).toHaveValue('');
  await user().selectOptions(
    screen.getByLabelText('Частота использования'),
    'DAILY',
  );
  await user().type(screen.getByLabelText('Страна покупки (код)'), 'KZ');
  await user().type(screen.getByLabelText('Месячный бюджет на весь AI'), '0');
  await submit();
  expect(sent).toHaveLength(0);
  expect(screen.getByLabelText('Валюта бюджета')).toBeVisible();
  expect(screen.getByLabelText('Валюта бюджета')).toHaveValue('');
  expect(screen.getByLabelText('Доступная предоплата')).toHaveValue('');
  expect(screen.getByLabelText('Студенческий статус')).toHaveValue('NONE');
  await user().type(screen.getByLabelText('Валюта бюджета'), 'USD');
  await user().type(screen.getByLabelText('Доступная предоплата'), '0');
  await user().type(screen.getByLabelText('Язык интерфейса (код)'), 'ru');
  await user().type(screen.getByLabelText('Способ покупки'), 'WEB');
  await submit();
  await screen.findByRole('heading', { name: 'Результаты подбора' });
  expect(sent[0].needs.map((n) => n.id)).toEqual([
    'study',
    'presentations',
    'coding',
  ]);
  expect(sent[0].needs.map((n) => n.intensity)).toEqual([
    'DAILY',
    'DAILY',
    'DAILY',
  ]);
  expect(sent[0].studentStatus).toBe('NONE');
});

test('conditional-only response is not presented as either a confirmed match or an empty search', async () => {
  const response = result(0);
  response.status = 'CONDITIONAL_ONLY';
  const b = result().bundles[0];
  b.eligibility = 'CONDITIONAL';
  b.strategies = [];
  b.costs.confirmedBudgetFit = false;
  b.costs.confidence = 'INCOMPLETE';
  response.conditionalAlternatives = [b];
  answer = async () => json(response);
  await start();
  await need();
  await finish();
  await submit();
  expect(
    await screen.findByText('Есть варианты, но условия нужно проверить.'),
  ).toBeVisible();
  expect(
    screen.queryByText('Пока не нашли подходящий вариант.'),
  ).not.toBeInTheDocument();
  expect(screen.queryByText(/FREE ·/)).not.toBeInTheDocument();
  expect(screen.getAllByTestId('bundle-card')).toHaveLength(1);
});
