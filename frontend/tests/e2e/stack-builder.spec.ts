import { expect, test, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import path from 'node:path';
import type { RecommendationResult } from '../../src/api/generated/types.gen';
const root = path.resolve('..');
const run = process.env.COMPASS_E2E_DIR;
if (!run)
  throw new Error(
    'Use bash scripts/test-e2e.sh: isolated PostgreSQL is mandatory.',
  );
function command(operation: string, ...args: string[]) {
  return execFileSync(
    'java',
    [
      '-jar',
      path.join(root, 'backend/target/ai-compass-0.1.0.jar'),
      `--catalog-command=${operation}`,
      '--spring.main.banner-mode=off',
      '--logging.level.root=ERROR',
      ...args,
    ],
    { encoding: 'utf8', env: process.env, timeout: 30000 },
  );
}
function publish(file: string) {
  const manifest = JSON.parse(readFileSync(path.join(root, file), 'utf8'));
  manifest.versionId = randomUUID();
  if (manifest.datasetKind === 'DEMO') {
    manifest.capabilities.push({
      id: 'terminal-agent',
      category: 'FUNCTION',
      name: 'Synthetic terminal agent',
    });
    manifest.evidence.push({
      ...manifest.evidence.find(
        (e: { id: string }) => e.id === 'terminal-coding-e',
      ),
      id: 'terminal-agent-e',
      summary: 'Synthetic terminal agent availability',
    });
    manifest.capabilityAssertions.push({
      id: 'terminal-agent',
      planId: 'terminal-plan',
      productId: null,
      capabilityId: 'terminal-agent',
      state: 'SUPPORTED',
      conditions: 'Synthetic terminal execution, test only',
      evidenceId: 'terminal-agent-e',
    });
  }
  const target = path.join(run!, `${manifest.versionId}.json`);
  writeFileSync(target, JSON.stringify(manifest), { mode: 0o600 });
  const hash = command('import', `--file=${target}`).match(
    /hash=([a-f0-9]{64})/,
  )?.[1];
  if (!hash) throw new Error('Missing manifest hash');
  command(
    'review',
    `--version=${manifest.versionId}`,
    `--hash=${hash}`,
    '--actor=p05-e2e',
  );
  const revision = command('current').match(/revision=(\d+)/)?.[1];
  if (!revision) throw new Error('Missing revision');
  command(
    'publish',
    `--version=${manifest.versionId}`,
    `--expected-revision=${revision}`,
    '--actor=p05-e2e',
    '--reason=Isolated UI acceptance',
  );
}
async function choose(page: Page, names = ['Synthetic study'], budget = '20') {
  await page.goto('/onboarding');
  for (const name of names) {
    await page.getByLabel(name, { exact: true }).check();
    await page
      .getByRole('combobox', { name: `Частота: ${name}`, exact: true })
      .selectOption('WEEKLY');
  }
  await page.getByRole('button', { name: 'Продолжить' }).click();
  await page.getByLabel('Страна покупки (код)').fill('KZ');
  await page.getByLabel('Язык интерфейса (код)').fill('ru');
  await page.getByLabel('Способ покупки').fill('WEB');
  await page.getByRole('button', { name: 'Продолжить' }).click();
  await expect(page.getByRole('alert')).toHaveCount(0);
  await page.getByLabel('Валюта бюджета').fill('USD');
  await page.getByLabel('Месячный бюджет на весь AI').fill(budget);
  await page.getByLabel('Доступная предоплата').fill('30');
  await expect(page.getByRole('alert')).toHaveCount(0);
}
async function preview(page: Page) {
  const responsePromise = page.waitForResponse((r) =>
    r.url().endsWith('/api/v1/recommendations/preview'),
  );
  await page.getByRole('button', { name: 'Подобрать мой AI-набор' }).click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  const data: RecommendationResult = await response.json();
  await expect(
    page.getByRole('heading', { name: 'Результаты подбора', exact: true }),
  ).toBeVisible();
  await expect(page.getByTestId('bundle-card')).toHaveCount(
    data.bundles.length + data.conditionalAlternatives.length,
  );
  return data;
}
async function noOverflow(page: Page) {
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true);
}

test.describe.serial('P05 full UI with real preview API', () => {
  test.setTimeout(120000);
  test.use({ actionTimeout: 15000 });
  test.beforeAll(async () => {
    test.setTimeout(120000);
    publish('data/fixtures/recommendation-demo.json');
    // The previous API suite uses the same isolated backend/IP. Let its real
    // fixed rate window expire; do not disable or reset the production limiter.
    await new Promise((resolve) => setTimeout(resolve, 60000));
  });
  test('student with several needs gets actual bundles, sources and product navigation', async ({
    page,
  }) => {
    await choose(page, [
      'Synthetic study',
      'Synthetic coding',
      'Synthetic presentations',
    ]);
    await page.getByLabel('Студенческий статус').selectOption('SELF_DECLARED');
    const data = await preview(page);
    expect(data.bundles.length).toBeGreaterThan(0);
    expect(data.bundles[0].items.map((i) => i.productId).sort()).toEqual([
      'learner',
      'slides',
    ]);
    await expect(page.getByText(/FREE ·/).first()).toBeVisible();
    await page.screenshot({
      path: path.join(run!, 'p05-student-results.png'),
      fullPage: true,
    });
    await page
      .getByTestId('bundle-card')
      .first()
      .getByRole('link', { name: 'Synthetic learner' })
      .click();
    await expect(
      page.getByRole('heading', {
        name: 'Synthetic learner',
        exact: true,
        level: 1,
      }),
    ).toBeVisible();
    await page
      .getByRole('link', { name: 'Собрать набор', exact: true })
      .click();
    await expect(page.getByLabel('Студенческий статус')).toHaveValue(
      'SELF_DECLARED',
    );
    await expect(page.getByLabel('Месячный бюджет на весь AI')).toHaveValue(
      '20',
    );
  });
  test('zero budget keeps a confirmed free choice and no paid upsell', async ({
    page,
  }) => {
    await choose(page, ['Synthetic study'], '0');
    const data = await preview(page);
    expect(data.bundles.length).toBeGreaterThan(0);
    expect(data.bundles.every((b) => b.costs.recurringMonthly === '0')).toBe(
      true,
    );
  });
  test('existing mustKeep subscription is retained and charged once', async ({
    page,
  }) => {
    await choose(page, ['Synthetic images']);
    await page
      .getByRole('button', { name: 'Добавить текущую подписку' })
      .click();
    await page.getByLabel('Тариф и предложение').selectOption('creator-offer');
    await page.getByLabel('Валюта платежа').fill('USD');
    await page.getByLabel('Фактический платёж').fill('15');
    await page.getByLabel('Период оплаты').selectOption('MONTH');
    const today = new Date().toISOString().slice(0, 10);
    await page.getByLabel('Исходная дата продления').fill(today);
    await page.getByLabel('Следующее продление').fill(today);
    await page.getByLabel('Часовой пояс платежа').fill('UTC');
    await page.getByLabel('Обязательно сохранить (mustKeep)').check();
    const data = await preview(page);
    expect(data.bundles.length).toBeGreaterThan(0);
    expect(
      data.bundles[0].items.filter((i) => i.planId === 'creator-plan'),
    ).toHaveLength(1);
    expect(data.bundles[0].items[0].action).toBe('KEEP');
    expect(data.bundles[0].costs.recurringMonthly).toBe('15');
  });
  test('no match is honest and editing budget triggers a fresh server result', async ({
    page,
  }) => {
    await choose(page, ['Synthetic video'], '0');
    const first = await preview(page);
    expect(first.bundles).toHaveLength(0);
    await expect(
      page.getByRole('heading', {
        name: 'Подходящий подтверждённый набор не найден',
      }),
    ).toBeVisible();
    await page.getByRole('link', { name: 'Изменить параметры' }).click();
    await page.getByLabel('Месячный бюджет на весь AI').fill('20');
    const next = await preview(page);
    expect(next.requestId).not.toBe(first.requestId);
    expect(next.bundles.length).toBeGreaterThan(0);
  });
  test('API error preserves parameters and retry uses real backend', async ({
    page,
  }) => {
    await choose(page);
    await page.route(
      '**/api/v1/recommendations/preview',
      (route) =>
        route.fulfill({
          status: 500,
          contentType: 'application/problem+json',
          body: '{"status":500}',
        }),
      { times: 1 },
    );
    await page.getByRole('button', { name: 'Подобрать мой AI-набор' }).click();
    await expect(page.getByRole('alert')).toContainText('Сервис временно');
    await expect(page.getByTestId('bundle-card')).toHaveCount(0);
    expect((await preview(page)).bundles.length).toBeGreaterThan(0);
  });
  test('429 shows pause and permits retry after Retry-After', async ({
    page,
  }) => {
    await choose(page);
    await page.route(
      '**/api/v1/recommendations/preview',
      (route) =>
        route.fulfill({
          status: 429,
          headers: { 'Retry-After': '2' },
          contentType: 'application/problem+json',
          body: '{"status":429}',
        }),
      { times: 1 },
    );
    await page.getByRole('button', { name: 'Подобрать мой AI-набор' }).click();
    await expect(page.getByRole('alert')).toContainText(
      'Слишком много запросов',
    );
    await expect(
      page.getByRole('button', { name: 'Подобрать мой AI-набор' }),
    ).toBeDisabled();
    await expect(
      page.getByRole('button', { name: 'Подобрать мой AI-набор' }),
    ).toBeEnabled({ timeout: 5000 });
    expect((await preview(page)).bundles.length).toBeGreaterThan(0);
  });
  test('Linux developer requires a terminal agent, not a web-only substitute', async ({
    page,
  }) => {
    await choose(page, ['Synthetic coding']);
    await page
      .getByRole('button', { name: '2. Условия и возможности' })
      .click();
    await page.getByLabel('Linux', { exact: true }).check();
    await page.getByLabel('Synthetic terminal agent', { exact: true }).check();
    await page.getByRole('button', { name: '3. Бюджет и подписки' }).click();
    const data = await preview(page);
    expect(data.bundles.length).toBeGreaterThan(0);
    expect(
      data.bundles.every((b) =>
        b.items.some(
          (i) =>
            i.productId === 'terminal' &&
            i.includedAgentIds.includes('included-agent'),
        ),
      ),
    ).toBe(true);
    await expect(
      page.getByText(/Включённые агенты: Synthetic Included Agent/).first(),
    ).toBeVisible();
  });
  test('editor sets actual monthly actions against a shared credit pool', async ({
    page,
  }) => {
    await choose(page, ['Synthetic images', 'Synthetic video']);
    await page
      .getByRole('button', { name: '2. Условия и возможности' })
      .click();
    await page.getByLabel('Synthetic editing', { exact: true }).check();
    await page
      .getByText('Уточнить месячный объём действий', { exact: true })
      .click();
    await page
      .getByLabel('Synthetic image · SYNTHETIC_DEFAULT', { exact: true })
      .check();
    await page
      .getByLabel('Действий за месяц: Synthetic image · SYNTHETIC_DEFAULT')
      .fill('10');
    await page
      .getByLabel('Synthetic video · SYNTHETIC_DEFAULT', { exact: true })
      .check();
    await page
      .getByLabel('Действий за месяц: Synthetic video · SYNTHETIC_DEFAULT')
      .fill('4');
    await page.getByRole('button', { name: '3. Бюджет и подписки' }).click();
    const overQuota = await preview(page);
    // 10*10 + 4*25 = 200 credits exceeds the ONE shared pool of 100.
    expect(overQuota.bundles).toHaveLength(0);
    await expect(
      page.getByRole('heading', {
        name: 'Подходящий подтверждённый набор не найден',
      }),
    ).toBeVisible();
    await page.getByRole('link', { name: 'Изменить параметры' }).click();
    await page
      .getByRole('button', { name: '2. Условия и возможности' })
      .click();
    await page
      .getByText('Уточнить месячный объём действий', { exact: true })
      .click();
    await page
      .getByLabel('Действий за месяц: Synthetic image · SYNTHETIC_DEFAULT')
      .fill('5');
    await page
      .getByLabel('Действий за месяц: Synthetic video · SYNTHETIC_DEFAULT')
      .fill('2');
    await page.getByRole('button', { name: '3. Бюджет и подписки' }).click();
    // 5*10 + 2*25 = 100 credits fits exactly, with no double counting.
    const data = await preview(page);
    expect(data.bundles.length).toBeGreaterThan(0);
    expect(data.bundles[0].items.some((i) => i.productId === 'creator')).toBe(
      true,
    );
    expect(data.bundles[0].usageFit).not.toBe('UNKNOWN');
  });
  for (const width of [360, 768, 1440])
    test(`responsive keyboard flow at ${width}px`, async ({ page }) => {
      const consoleErrors: string[] = [];
      page.on('pageerror', (e) => consoleErrors.push(e.message));
      page.on('console', (m) => {
        if (m.type() === 'error') consoleErrors.push(m.text());
      });
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/');
      await expect(
        page.getByRole('heading', { name: /Ваш набор AI/ }),
      ).toBeVisible();
      await noOverflow(page);
      await page.screenshot({
        path: path.join(run!, `p05-home-${width}.png`),
        fullPage: true,
      });
      await choose(page);
      await noOverflow(page);
      await page.screenshot({
        path: path.join(run!, `p05-form-${width}.png`),
        fullPage: true,
      });
      await page
        .getByRole('button', { name: 'Подобрать мой AI-набор' })
        .focus();
      await expect(
        page.getByRole('button', { name: 'Подобрать мой AI-набор' }),
      ).toBeFocused();
      await preview(page);
      await noOverflow(page);
      await page.screenshot({
        path: path.join(run!, `p05-results-${width}.png`),
        fullPage: true,
      });
      await page.getByRole('link', { name: 'Каталог', exact: true }).click();
      await expect(
        page.getByRole('heading', { name: 'Каталог AI' }),
      ).toBeVisible();
      await noOverflow(page);
      await page.screenshot({
        path: path.join(run!, `p05-catalog-${width}.png`),
        fullPage: true,
      });
      expect(consoleErrors).toEqual([]);
    });
  test('limited real catalog cannot invent a video category or recommendations', async ({
    page,
  }) => {
    publish('data/catalog/catalog-2026-11-14-r2.json');
    await page.goto('/onboarding');
    await expect(
      page.getByText('Разработка программ', { exact: true }),
    ).toBeVisible();
    expect(await page.getByRole('checkbox').count()).toBe(3);
    await page
      .getByLabel('В каталоге нет одной из моих обязательных потребностей')
      .check();
    await page.getByRole('button', { name: 'Продолжить' }).click();
    await expect(page.getByRole('alert')).toContainText(
      'недостаточно подтверждённых данных',
    );
    await expect(page.getByTestId('bundle-card')).toHaveCount(0);
    await page.getByRole('link', { name: 'Результаты', exact: true }).click();
    await expect(
      page.getByText('Сначала расскажите о потребностях'),
    ).toBeVisible();
  });
  test('real regional UNKNOWN remains distinct from FREE', async ({ page }) => {
    await choose(page, ['Разработка программ']);
    const data = await preview(page);
    expect(data.bundles).toHaveLength(0);
    expect(data.conditionalAlternatives.length).toBeGreaterThan(0);
    expect(
      data.conditionalAlternatives.every(
        (b) => b.costs.recurringMonthly === null && !b.costs.confirmedBudgetFit,
      ),
    ).toBe(true);
    await expect(page.getByText('Цена не подтверждена').first()).toBeVisible();
    await expect(page.getByText(/FREE ·/)).toHaveCount(0);
  });
});
