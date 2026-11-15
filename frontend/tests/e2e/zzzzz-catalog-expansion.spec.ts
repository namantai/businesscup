import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import type { RecommendationResult } from '../../src/api/generated/types.gen';

if (!process.env.COMPASS_E2E_DIR)
  throw new Error('Use isolated scripts/test-e2e.sh');
const root = path.resolve('..');
const previous = '03000000-0000-4000-8000-000000000002';
const expanded = 'ce000000-0000-4000-8000-000000000001';
let hash = '';
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
    { encoding: 'utf8', timeout: 30000, env: process.env },
  );
}
function load(file: string) {
  const result = command(
    'import',
    `--file=${path.join(root, 'data/catalog', file)}`,
  );
  const value = result.match(/hash=([a-f0-9]{64})/)?.[1];
  if (!value) throw new Error('Missing content hash');
  return value;
}
function review(id: string, value: string) {
  command(
    'review',
    `--version=${id}`,
    `--hash=${value}`,
    '--actor=ce-isolated-e2e',
  );
}
function activate(id: string, rollback = false) {
  const revision = command('current').match(/revision=(\d+)/)?.[1];
  if (!revision) throw new Error('Missing current revision');
  command(
    rollback ? 'rollback' : 'publish',
    `--version=${id}`,
    `--expected-revision=${revision}`,
    '--actor=ce-isolated-e2e',
    '--reason=Reviewed catalog in isolated database only',
  );
}

test.describe.serial('CE reviewed expansion with real backend', () => {
  test.setTimeout(120000);
  test.use({ actionTimeout: 15000 });
  test.beforeAll(() => {
    review(previous, load('catalog-2026-11-14-r2.json'));
    activate(previous);
    hash = load('catalog-2026-11-14-ce1.json');
  });
  test('reviewed expansion activates API and UI and preserves pinned r2', async ({
    page,
    request,
  }) => {
    expect(
      (
        await request.get(`/api/v1/catalog/products?version=${expanded}`)
      ).status(),
    ).toBe(404);
    review(expanded, hash);
    expect(
      (
        await request.get(`/api/v1/catalog/products?version=${expanded}`)
      ).status(),
    ).toBe(404);
    activate(expanded);
    await page.goto('/catalog?size=24');
    await expect(page.getByText('Найдено: 23', { exact: true })).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Higgsfield', exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Источник цены', exact: true }).first(),
    ).toBeVisible();
    await page.screenshot({
      path: path.join(process.env.COMPASS_E2E_DIR!, 'ux-catalog.png'),
      fullPage: true,
    });
    expect(
      (
        await (
          await request.get(`/api/v1/catalog/products?version=${previous}`)
        ).json()
      ).total,
    ).toBe(3);
    await page.goto(`/catalog/chatgpt?version=${previous}`);
    await expect(
      page.getByRole('heading', { name: 'ChatGPT', exact: true, level: 1 }),
    ).toBeVisible();
  });
  test('category search, evidence-backed price and region filters paginate on server', async ({
    page,
  }) => {
    await page.goto('/catalog?size=2');
    await expect(page.getByText('Страница 1 из 12')).toBeVisible();
    await page.getByLabel('Базовая цена').selectOption('FREE');
    await page.getByLabel('Регион наблюдения цены').selectOption('kz');
    await expect(page.getByText('Найдено: 0', { exact: true })).toBeVisible();
    await page.getByLabel('Базовая цена').selectOption('UNKNOWN');
    await expect(page.getByText('Найдено: 23', { exact: true })).toHaveCount(0);
    await expect(
      page.getByRole('button', { name: 'Далее', exact: true }),
    ).toBeEnabled();
    await page.goto('/catalog?capabilityId=video-generation&q=Luma');
    await expect(
      page.getByRole('link', { name: 'Luma Dream Machine', exact: true }),
    ).toBeVisible();
    await page.goto('/catalog?priceKind=FIXED&regionId=cn&currency=CNY');
    await expect(page.getByText('Найдено: 1', { exact: true })).toBeVisible();
    await expect(
      page.getByRole('link', { name: 'Kimi', exact: true }),
    ).toBeVisible();
  });
  test('representative category details show sources and included agents separately', async ({
    page,
  }) => {
    for (const [slug, name] of [
      ['gemini', 'Gemini'],
      ['cursor', 'Cursor'],
      ['perplexity', 'Perplexity'],
      ['canva', 'Canva'],
      ['leonardo', 'Leonardo AI'],
      ['runway', 'Runway'],
      ['elevenlabs', 'ElevenLabs'],
      ['suno', 'Suno'],
      ['descript', 'Descript'],
      ['kimi', 'Kimi'],
    ]) {
      await page.goto(`/catalog/${slug}?version=${expanded}`);
      await expect(
        page.getByRole('heading', { name, level: 1, exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole('heading', {
          name: 'Источники и проверенные утверждения',
        }),
      ).toBeVisible();
      expect(
        await page.locator('a[target="_blank"][href^="https://"]').count(),
      ).toBeGreaterThan(0);
    }
    await expect(
      page.getByRole('listitem').filter({ hasText: 'Kimi Code' }).first(),
    ).toBeVisible();
    await page.goto(`/catalog/midjourney?version=${expanded}`);
    await expect(page.getByText('96 USD / год', { exact: true })).toBeVisible();
    await expect(page.getByText(/8 USD; оплата за год сразу/)).toBeVisible();
    await page.getByLabel('Регион предложения').selectOption('kz');
    await expect(
      page.getByText('Цена неизвестна (UNKNOWN)', { exact: true }).first(),
    ).toBeVisible();
    await expect(page.getByText('96 USD / год', { exact: true })).toHaveCount(
      0,
    );
  });
  for (const [label, names] of [
    [
      'student',
      ['Учёба и понимание материала', 'Разработка программ', 'Презентации'],
    ],
    [
      'creator',
      [
        'Генерация изображений',
        'Генерация видео',
        'Монтаж видео',
        'Голос и озвучка',
      ],
    ],
    ['researcher', ['Поиск информации']],
  ] as const) {
    test(`real ${label} profile uses published taxonomy and Java preview`, async ({
      page,
    }) => {
      await page.goto('/onboarding');
      await page.getByRole('button', { name: /Своё/ }).click();
      for (const name of names) {
        await page.getByLabel(name, { exact: true }).check();
      }
      await page.getByText('Дополнительные настройки', { exact: true }).click();
      await page.getByLabel('Страна покупки (код)').fill('US');
      await page.getByLabel('Частота использования').selectOption('WEEKLY');
      await page.getByLabel('Язык интерфейса (код)').fill('en');
      await page.getByLabel('Способ покупки').fill('WEB');
      await expect(page.getByRole('alert')).toHaveCount(0);
      await page.getByLabel('Валюта бюджета').selectOption('USD');
      await page.getByLabel('Бюджет в месяц').fill('100');
      await page.getByLabel('Доступная предоплата').fill('300');
      const response = page.waitForResponse((r) =>
        r.url().endsWith('/api/v1/recommendations/preview'),
      );
      await page.getByRole('button', { name: 'Подобрать AI-набор' }).click();
      const http = await response;
      expect(http.status()).toBe(200);
      const result: RecommendationResult = await http.json();
      expect(result.catalogVersion).toBe(expanded);
      expect(result.datasetKind).toBe('REVIEWED');
      expect(result.status).toBe('CONDITIONAL_ONLY');
      expect(result.bundles).toHaveLength(0);
      expect(result.conditionalAlternatives.length).toBeGreaterThan(0);
      expect(
        result.conditionalAlternatives.every(
          (b) => !b.costs.confirmedBudgetFit && b.items.length <= 4,
        ),
      ).toBe(true);
      await expect(page.getByTestId('bundle-card')).toHaveCount(
        result.conditionalAlternatives.length,
      );
      console.log(
        `CE ${label}: ${result.status}; ${result.conditionalAlternatives[0].items.map((i) => i.productId).join(',')}`,
      );
    });
  }
  for (const width of [360, 768, 1440]) {
    test(`expanded catalog accessible layout at ${width}`, async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', (e) => errors.push(e.message));
      page.on('console', (m) => {
        if (m.type() === 'error') errors.push(m.text());
      });
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/catalog?size=24');
      await expect(
        page.getByText('Найдено: 23', { exact: true }),
      ).toBeVisible();
      await page.keyboard.press('Tab');
      await expect(
        page.getByRole('link', { name: 'Перейти к содержимому' }),
      ).toBeFocused();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: path.join(
          process.env.COMPASS_E2E_DIR!,
          `ce-catalog-${width}.png`,
        ),
        fullPage: true,
      });
      await page.goto(`/catalog/higgsfield?version=${expanded}`);
      await expect(
        page.getByRole('heading', { name: 'Higgsfield', level: 1 }),
      ).toBeVisible();
      await expect(
        page.getByText(/нет дневных\/месячных credits/).first(),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      expect(errors).toEqual([]);
      await page.screenshot({
        path: path.join(process.env.COMPASS_E2E_DIR!, `ce-detail-${width}.png`),
        fullPage: true,
      });
    });
  }
  for (const [role, remove, budget, goals] of [
    [
      'Студент + код Учиться и создавать',
      ['Поиск информации'],
      '20',
      ['study', 'development', 'presentations'],
    ],
    [
      'Контент-креатор Изображения, видео и звук',
      ['Монтаж видео', 'Генерация музыки'],
      '50',
      ['images', 'video', 'voice'],
    ],
  ] as const) {
    test(`single-page preset remains editable: ${role}`, async ({ page }) => {
      await page.setViewportSize({
        width: budget === '20' ? 1440 : 390,
        height: 1000,
      });
      await page.goto('/onboarding');
      const preset = page.getByRole('button', { name: role, exact: true });
      await preset.click();
      for (const name of remove)
        await page.getByRole('checkbox', { name, exact: true }).uncheck();
      await expect(preset).toHaveAttribute('aria-pressed', 'true');
      await expect(
        page.getByText(/Основа:.*Цели изменены вручную/),
      ).toBeVisible();
      await expect(
        page.getByText('Точная настройка целей').locator('..'),
      ).not.toHaveAttribute('open');
      await page.getByLabel('Бюджет в месяц').fill(budget);
      await page.getByLabel('Валюта бюджета').selectOption('USD');
      await page.getByText('Дополнительные настройки', { exact: true }).click();
      await page.getByLabel('Страна покупки (код)').fill('US');
      await page.getByLabel('Частота использования').selectOption('WEEKLY');
      await page.getByLabel('Доступная предоплата').fill(budget);
      await page.getByLabel('Язык интерфейса (код)').fill('en');
      await page.getByLabel('Способ покупки').fill('WEB');
      await page.getByText('Дополнительные настройки', { exact: true }).click();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: path.join(process.env.COMPASS_E2E_DIR!, `ux-form-${budget}.png`),
        fullPage: true,
      });
      const pending = page.waitForResponse((r) =>
        r.url().endsWith('/api/v1/recommendations/preview'),
      );
      await page.getByRole('button', { name: 'Подобрать AI-набор' }).click();
      const response = await pending;
      expect(response.status()).toBe(200);
      expect(
        response
          .request()
          .postDataJSON()
          .needs.map((n: { id: string }) => n.id)
          .sort(),
      ).toEqual([...goals].sort());
      expect(response.request().postDataJSON().budget.monthlyMax).toBe(budget);
      const result: RecommendationResult = await response.json();
      expect(result.catalogVersion).toBe(expanded);
      expect(result.datasetKind).toBe('REVIEWED');
      expect(result.status).toBe('CONDITIONAL_ONLY');
      expect(result.bundles).toHaveLength(0);
      await expect(page.getByTestId('bundle-card')).toHaveCount(
        result.conditionalAlternatives.length,
      );
      await expect(page.getByText(/От 0 USD \/ мес\./).first()).toBeVisible();
      await expect(
        page.getByRole('link', { name: 'Источник цены', exact: true }).first(),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await page
        .getByTestId('bundle-card')
        .first()
        .screenshot({
          path: path.join(
            process.env.COMPASS_E2E_DIR!,
            `ux-result-${budget}.png`,
          ),
        });
      await page.getByRole('link', { name: 'Изменить параметры' }).click();
      await expect(preset).toHaveAttribute('aria-pressed', 'true');
      await expect(page.getByLabel('Бюджет в месяц')).toHaveValue(budget);
    });
  }
  for (const width of [360, 1440]) {
    test(`Quick Match shows all real products and instant goals at ${width}px`, async ({
      page,
    }) => {
      await page.setViewportSize({ width, height: 1000 });
      const errors: string[] = [];
      page.on('pageerror', (e) => errors.push(e.message));
      await page.goto('/');
      const directory = page.getByRole('region', { name: 'Весь каталог' });
      await expect(directory.getByRole('link')).toHaveCount(23);
      await page
        .getByRole('button', { name: 'Студент + код', exact: true })
        .click();
      await page.getByLabel('Месячный бюджет').fill('20');
      await page.getByLabel('Валюта бюджета').selectOption('USD');
      for (const name of [
        'Учёба',
        'Программирование',
        'Презентации',
        'Поиск и анализ',
      ]) {
        await expect(
          page.getByRole('button', { name: name, exact: true }),
        ).toHaveAttribute('aria-pressed', 'true');
      }
      const matched = page.getByRole('region', {
        name: 'Подходящие по задачам',
      });
      expect(await matched.getByRole('article').count()).toBeGreaterThan(0);
      await expect(
        matched.getByRole('link', { name: /Источник/ }).first(),
      ).toBeVisible();
      await expect(
        page.getByText(/Общая стоимость и совместимость не рассчитаны/),
      ).toBeVisible();
      await page
        .getByRole('button', { name: 'Презентации', exact: true })
        .click();
      await expect(
        page.getByRole('button', { name: 'Студент + код', exact: true }),
      ).toHaveAttribute('aria-pressed', 'true');
      await page
        .getByRole('button', { name: 'Презентации', exact: true })
        .click();
      await page.getByLabel('Месячный бюджет').clear();
      await expect(page.getByRole('alert')).toBeVisible();
      await expect(directory.getByRole('link')).toHaveCount(23);
      await page.getByLabel('Месячный бюджет').fill('20');
      await expect(page.getByRole('alert')).toHaveCount(0);
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      expect(errors).toEqual([]);
      await page.screenshot({
        path: path.join(
          process.env.COMPASS_E2E_DIR!,
          `quick-match-${width}.png`,
        ),
        fullPage: true,
      });
    });
  }
  test('rollback restores r2 while expanded links remain readable', async ({
    page,
    request,
  }) => {
    activate(previous, true);
    await page.goto('/catalog');
    await expect(page.getByText('Найдено: 3', { exact: true })).toBeVisible();
    expect(
      (
        await (
          await request.get(
            `/api/v1/catalog/products?version=${expanded}&size=100`,
          )
        ).json()
      ).total,
    ).toBe(23);
    await page.goto(`/catalog/canva?version=${expanded}`);
    await expect(
      page.getByRole('heading', { name: 'Canva', level: 1, exact: true }),
    ).toBeVisible();
  });
});
