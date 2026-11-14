import { test, expect, type Page } from '@playwright/test';
import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import path from 'node:path';
import type { SubscriptionTerms } from '../../src/api/generated/types.gen';
test.use({ trace: 'off', video: 'off' });
const run = process.env.COMPASS_E2E_DIR;
const root = path.resolve('..');
if (!run || process.env.DB_NAME !== 'compass_e2e')
  throw new Error('Isolated P07 database required');
const password = 'Synthetic-ledger-password-986!';
const today = new Date().toISOString().slice(0, 10);
let version: string;
function command(op: string, ...args: string[]) {
  return execFileSync(
    'java',
    [
      '-jar',
      path.join(root, 'backend/target/ai-compass-0.1.0.jar'),
      `--catalog-command=${op}`,
      '--spring.main.banner-mode=off',
      '--logging.level.root=ERROR',
      ...args,
    ],
    { encoding: 'utf8', env: process.env, timeout: 30000 },
  );
}
async function send(page: Page, method: string, route: string, data?: unknown) {
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  return page.request.fetch('/api/v1' + route, {
    method,
    data,
    headers: { [csrf.headerName]: csrf.token },
  });
}
async function account(page: Page, editor = false) {
  const email = `${randomUUID()}@example.invalid`;
  expect(
    (await send(page, 'POST', '/auth/register', { email, password })).status(),
  ).toBe(202);
  let token = '';
  for (const file of readdirSync(path.join(run!, 'mail'))) {
    const message = readFileSync(path.join(run!, 'mail', file), 'utf8');
    if (message.startsWith(`To: ${email}\nPurpose: VERIFY\n`))
      token = message.split('#token=')[1].trim();
  }
  expect(!!token).toBe(true);
  expect((await send(page, 'POST', '/auth/verify', { token })).status()).toBe(
    200,
  );
  expect(
    (await send(page, 'POST', '/auth/login', { email, password })).status(),
  ).toBe(200);
  const profile = JSON.parse(
    readFileSync(
      path.join(
        root,
        `data/fixtures/profiles/${editor ? 'editor' : 'student'}.json`,
      ),
      'utf8',
    ),
  );
  profile.asOf = null;
  profile.portfolioMode = 'REVIEW';
  expect(
    (
      await send(page, 'POST', '/profiles', {
        name: 'Synthetic needs',
        profile,
        revision: 0,
      })
    ).status(),
  ).toBe(201);
  return email;
}
function terms(name: string, amount: string | null = '12'): SubscriptionTerms {
  return {
    name,
    status: 'ACTIVE',
    catalogVersion: null,
    planId: null,
    offerId: null,
    currency: 'USD',
    amount,
    priceOrigin: amount === null ? 'UNKNOWN' : 'USER_REPORTED',
    billingPeriod: 'MONTH',
    renewalAnchor: today,
    nextRenewal: today,
    billingZone: 'UTC',
    paidFrom: null,
    paidUntil: null,
    paidAmount: null,
    autoRenew: 'YES',
    trialUntil: null,
    obligation: 'NONE',
    commitmentUntil: null,
    cancellationAvailableFrom: today,
    additionalPerPeriod: '0',
    transitionCost: '0',
    mustKeep: false,
  };
}
async function add(page: Page, t: SubscriptionTerms) {
  const r = await send(page, 'POST', '/subscriptions', {
    terms: t,
    revision: 0,
  });
  expect(r.status()).toBe(201);
  return r.json();
}
async function compare(page: Page) {
  await page.getByRole('button', { name: 'Рассчитать и сравнить' }).click();
  await expect(
    page.getByRole('region', { name: 'Сравнение наборов' }),
  ).toBeVisible();
}
test.describe('P07 isolated subscription optimizer', () => {
  test.setTimeout(150000);
  test.beforeAll(async () => {
    const manifest = JSON.parse(
      readFileSync(
        path.join(root, 'data/fixtures/recommendation-demo.json'),
        'utf8',
      ),
    );
    version = randomUUID();
    manifest.versionId = version;
    const file = path.join(run!, `${version}.json`);
    writeFileSync(file, JSON.stringify(manifest), { mode: 0o600 });
    const hash = command('import', `--file=${file}`).match(
      /hash=([a-f0-9]{64})/,
    )?.[1];
    if (!hash) throw new Error('Missing hash');
    command(
      'review',
      `--version=${version}`,
      `--hash=${hash}`,
      '--actor=p07-e2e',
    );
    const revision = command('current').match(/revision=(\d+)/)?.[1];
    command(
      'publish',
      `--version=${version}`,
      `--expected-revision=${revision}`,
      '--actor=p07-e2e',
      '--reason=Isolated optimizer tests',
    );
  });
  // Existing preview/CSRF limit stays enabled; independent scenarios start in a fresh time window.
  test.beforeEach(async () => {
    await new Promise((r) => setTimeout(r, 61000));
  });
  test('UI add, actual price edit, mustKeep, delete record, three-subscription comparison', async ({
    page,
  }) => {
    await account(page);
    await page.goto('/subscriptions');
    await page.getByRole('button', { name: 'Добавить подписку' }).click();
    await page
      .getByLabel('Название', { exact: true })
      .fill('My synthetic tool');
    await page.getByLabel('Фактическая цена за период').fill('12');
    await page.getByLabel('Исходная дата продления').fill(today);
    await page.getByLabel('Дата следующего списания').fill(today);
    await page.getByLabel('Часовой пояс списания').fill('UTC');
    await page.getByLabel('Автопродление').selectOption('YES');
    await page
      .getByText('Оплаченный период, отмена и дополнительные расходы', {
        exact: true,
      })
      .click();
    await page.getByLabel('Обязательство по контракту').selectOption('NONE');
    await page.getByLabel('Можно прекратить будущие списания с').fill(today);
    await page
      .getByLabel('Дополнительно за период: налоги, сборы, кредиты')
      .fill('0');
    await page.getByLabel('Расходы на переход / отмену').fill('0');
    await page.getByRole('button', { name: 'Сохранить подписку' }).click();
    const card = page.getByRole('article', { name: 'My synthetic tool' });
    await expect(card).toContainText('12 USD');
    await card.getByRole('button', { name: 'Редактировать' }).click();
    await page.getByLabel('Фактическая цена за период').fill('10');
    await page.getByRole('button', { name: 'Сохранить подписку' }).click();
    await expect(card).toContainText('10 USD');
    await card.getByLabel('Обязательно сохранить', { exact: true }).check();
    await expect(
      card.getByLabel('Обязательно сохранить', { exact: true }),
    ).toBeChecked();
    await card.getByLabel('Обязательно сохранить', { exact: true }).uncheck();
    await expect(
      card.getByLabel('Обязательно сохранить', { exact: true }),
    ).toBeEnabled();
    await add(page, terms('Second synthetic', '8'));
    await add(page, terms('Third synthetic', '5'));
    await page.reload();
    await compare(page);
    await expect(
      page.getByRole('heading', { name: 'Есть вариант для рассмотрения' }),
    ).toBeVisible();
    const current = page.getByRole('article').filter({
      has: page.getByRole('heading', { name: 'Текущий набор', exact: true }),
    });
    await expect(current).toContainText('276 USD');
    await card.getByRole('button', { name: 'Удалить запись' }).click();
    await expect(card).toHaveCount(0);
    await expect(
      page.getByText(/Удаление записи в Compass не отменяет/),
    ).toBeVisible();
  });
  test('current cheaper, catalog prefill, annual prepaid access and product link', async ({
    page,
  }) => {
    await account(page, true);
    await page.goto('/subscriptions');
    await page.getByRole('button', { name: 'Добавить подписку' }).click();
    await page.getByLabel('Тариф из каталога').selectOption('creator-offer');
    await expect(page.getByLabel('Фактическая цена за период')).toHaveValue(
      '15',
    );
    await page.getByRole('button', { name: 'Закрыть форму' }).click();
    const entry = await add(page, {
      ...terms('Synthetic creator', '1'),
      catalogVersion: version,
      planId: 'creator-plan',
      offerId: 'creator-offer',
    });
    await page.reload();
    await compare(page);
    await expect(
      page.getByRole('heading', { name: 'Лучше ничего не менять' }),
    ).toBeVisible();
    await page
      .getByRole('link', { name: 'Карточка продукта и источники' })
      .click();
    await expect(page).toHaveURL(/\/catalog\/creator\?version=/);
    await page.goto('/subscriptions');
    const next = new Date(today + 'T00:00:00Z');
    next.setUTCFullYear(next.getUTCFullYear() + 1);
    const renewal = next.toISOString().slice(0, 10);
    expect(
      (
        await send(page, 'PATCH', `/subscriptions/${entry.id}`, {
          revision: 1,
          terms: {
            ...entry.terms,
            amount: '120',
            billingPeriod: 'YEAR',
            renewalAnchor: renewal,
            nextRenewal: renewal,
            paidFrom: today,
            paidUntil: renewal,
            paidAmount: '120',
          },
        })
      ).status(),
    ).toBe(200);
    await page.reload();
    await compare(page);
    const current = page.getByRole('article').filter({
      has: page.getByRole('heading', { name: 'Текущий набор', exact: true }),
    });
    await expect(current).toContainText('120 USD');
    await expect(current).toContainText('0 USD');
    await expect(current).toContainText('Уже оплачено — не экономия');
  });
  test('UNKNOWN, ownership A/B, export and account deletion', async ({
    page,
    browser,
  }) => {
    await account(page);
    const entry = await add(page, terms('Unknown actual price', null));
    await page.goto('/subscriptions');
    await compare(page);
    await expect(
      page.getByRole('heading', { name: 'Экономию пока нельзя определить' }),
    ).toBeVisible();
    await expect(
      page.getByRole('article', { name: 'Unknown actual price' }),
    ).toContainText('UNKNOWN');
    const context = await browser.newContext({
      baseURL: process.env.FRONTEND_PORT
        ? `http://127.0.0.1:${process.env.FRONTEND_PORT}`
        : undefined,
    });
    const other = await context.newPage();
    try {
      await account(other);
      expect(
        (await other.request.get(`/api/v1/subscriptions/${entry.id}`)).status(),
      ).toBe(404);
      expect(
        (
          await send(other, 'DELETE', `/subscriptions/${entry.id}?revision=1`)
        ).status(),
      ).toBe(404);
    } finally {
      await context.close();
    }
    const exported = await (await page.request.get('/api/v1/me/export')).json();
    expect(exported.subscriptions).toHaveLength(1);
    expect(exported.subscriptions[0].terms.amount).toBeNull();
    expect(
      (await send(page, 'DELETE', '/me', { password, confirm: true })).status(),
    ).toBe(204);
    expect((await page.request.get('/api/v1/subscriptions')).status()).toBe(
      401,
    );
  });
  test('responsive ledger and comparison 360 768 1440, no console errors or overflow', async ({
    page,
  }) => {
    await account(page);
    await add(
      page,
      terms(
        'Длинное синтетическое название подписки для проверки переноса',
        '12',
      ),
    );
    await page.goto('/subscriptions');
    await compare(page);
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    for (const width of [360, 768, 1440]) {
      await page.setViewportSize({ width, height: 960 });
      await expect(
        page.getByRole('heading', { name: 'Мои AI-подписки' }),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: path.join(run!, `p07-subscriptions-${width}.png`),
        fullPage: true,
      });
    }
    expect(errors).toEqual([]);
  });
});
