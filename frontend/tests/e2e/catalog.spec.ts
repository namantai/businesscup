import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import path from 'node:path';

const root = path.resolve('..');
const run = process.env.COMPASS_E2E_DIR;
if (!run)
  throw new Error('Run bash scripts/test-e2e.sh to get an isolated database.');
const firstId = randomUUID();
const secondId = randomUUID();
let firstHash = '';
let secondHash = '';
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
function review(id: string, hash: string) {
  command(
    'review',
    `--version=${id}`,
    `--hash=${hash}`,
    '--actor=e2e-fixture-review',
  );
}
function activate(id: string, revision: number, rollback = false) {
  command(
    rollback ? 'rollback' : 'publish',
    `--version=${id}`,
    `--expected-revision=${revision}`,
    '--actor=e2e-fixture-review',
    '--reason=Isolated synthetic acceptance scenario',
  );
}
function imported(id: string, name: string, amount: string) {
  const manifest = JSON.parse(
    readFileSync(path.join(root, 'data/fixtures/catalog-demo.json'), 'utf8'),
  );
  manifest.versionId = id;
  manifest.products[0].name = name;
  manifest.priceOffers[0].amount = amount;
  manifest.priceOffers[0].upfrontAmount = amount;
  manifest.products.push({
    ...manifest.products[0],
    id: 'demo-third',
    slug: 'demo-third',
    name: 'E2E third',
  });
  const file = path.join(run!, id + '.json');
  writeFileSync(file, JSON.stringify(manifest), { mode: 0o600 });
  const result = command('import', `--file=${file}`);
  const hash = result.match(/hash=([a-f0-9]{64})/)?.[1];
  if (!hash) throw new Error('Import did not return a canonical hash');
  return hash;
}

test.describe.serial('isolated published catalog', () => {
  test.setTimeout(120000);
  test.beforeAll(() => {
    firstHash = imported(firstId, 'E2E studio', '12.34');
  });

  test('import stays private; review and publish activate API and frontend', async ({
    page,
    request,
  }) => {
    expect(
      (
        await request.get(`/api/v1/catalog/products?version=${firstId}`)
      ).status(),
    ).toBe(404);
    await page.goto('/catalog');
    await expect(page.getByText(/Каталог пока не опубликован/)).toBeVisible();
    review(firstId, firstHash);
    expect(
      (
        await request.get(`/api/v1/catalog/products?version=${firstId}`)
      ).status(),
    ).toBe(404);
    activate(firstId, 0);
    await page.getByRole('button', { name: 'Обновить каталог' }).click();
    await expect(page.getByRole('link', { name: 'E2E studio' })).toBeVisible();
    const meta = await (await request.get('/api/v1/catalog/meta')).json();
    expect(meta.versionId).toBe(firstId);
    await page.getByRole('link', { name: 'E2E studio' }).click();
    await expect(
      page.getByRole('heading', { name: 'E2E studio', level: 1 }),
    ).toBeVisible();
    await expect(
      page.getByText('12.34 USD / месяц', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText('Бесплатный базовый тариф', { exact: true }).first(),
    ).toBeVisible();
    await expect(
      page.getByText('Цена неизвестна (UNKNOWN)', { exact: true }).first(),
    ).toBeVisible();
    await expect(page.getByText(/10.000834 USD/)).toBeVisible();
    await expect(
      page.getByRole('heading', {
        name: 'Источники и проверенные утверждения',
      }),
    ).toBeVisible();
    await page.getByLabel('Регион предложения').selectOption('demo-us');
    await expect(
      page.getByText('1234.56 KZT / месяц', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText('12.34 USD / месяц', { exact: true }),
    ).toHaveCount(0);
  });

  test('snapshot update and rollback switch UI while historical links remain reproducible', async ({
    page,
    request,
  }) => {
    secondHash = imported(secondId, 'E2E studio updated', '19.99');
    review(secondId, secondHash);
    activate(secondId, 1);
    await page.goto('/catalog');
    await expect(
      page.getByRole('link', { name: 'E2E studio updated' }),
    ).toBeVisible();
    await page.getByRole('link', { name: 'E2E studio updated' }).click();
    await expect(
      page.getByText('19.99 USD / месяц', { exact: true }),
    ).toBeVisible();
    await page.goto(`/catalog/demo-studio?version=${firstId}`);
    await expect(
      page.getByRole('heading', { level: 1, name: 'E2E studio', exact: true }),
    ).toBeVisible();
    activate(firstId, 2, true);
    await page.goto('/catalog');
    await expect(
      page.getByRole('link', { name: 'E2E studio', exact: true }),
    ).toBeVisible();
    expect(
      (await (await request.get('/api/v1/catalog/meta')).json()).revision,
    ).toBe(3);
    expect(
      (
        await request.get(`/api/v1/catalog/products?version=${secondId}`)
      ).status(),
    ).toBe(200);
  });

  test('search, filters, pagination and separate model/agent views', async ({
    page,
  }) => {
    await page.goto('/catalog?size=2');
    await expect(page.getByText('Страница 1 из 2')).toBeVisible();
    await page.getByRole('button', { name: 'Далее', exact: true }).click();
    await expect(page.getByText('Страница 2 из 2')).toBeVisible();
    await page.getByLabel('Тип продукта').selectOption('APPLICATION');
    await expect(page.getByText('Страница 1 из 1')).toBeVisible();
    await page.getByLabel('Поиск по названию').fill('no-such-product');
    await expect(page.getByText(/Ничего не найдено/)).toBeVisible();
    await page.getByLabel('Поиск по названию').fill('');
    await page.getByRole('button', { name: 'Модели', exact: true }).click();
    await expect(
      page.getByRole('heading', { name: 'Synthetic Model v1' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Агенты', exact: true }).click();
    await expect(
      page.getByRole('heading', { name: 'Synthetic Included Agent' }),
    ).toBeVisible();
  });

  test('public API cannot import, review, publish or rollback', async ({
    request,
  }) => {
    for (const operation of ['import', 'review', 'publish', 'rollback']) {
      expect(
        (
          await request.post(`/api/v1/catalog/${operation}`, { data: {} })
        ).status(),
      ).toBe(403);
      expect((await request.get(`/api/v1/catalog/${operation}`)).status()).toBe(
        404,
      );
      expect(
        (
          await request.post(`/api/v1/admin/catalog/${operation}`, { data: {} })
        ).status(),
      ).toBe(403);
    }
    const schema = await (await request.get('/api/v1/openapi')).json();
    expect(
      Object.keys(schema.paths).every(
        (p) => !p.includes('publish') && !p.includes('import'),
      ),
    ).toBe(true);
  });

  test('network failure has a retry state without replacing data', async ({
    page,
  }) => {
    await page.route('**/api/v1/catalog/products?**', (route) => route.abort());
    await page.goto('/catalog');
    await expect(page.getByRole('alert')).toBeVisible();
    await page.unroute('**/api/v1/catalog/products?**');
    await page.getByRole('button', { name: 'Повторить', exact: true }).click();
    await expect(
      page.getByRole('link', { name: 'E2E studio', exact: true }),
    ).toBeVisible();
  });

  for (const width of [360, 768, 1440]) {
    test(`catalog keyboard navigation and no overflow at ${width}px`, async ({
      page,
    }) => {
      const errors: string[] = [];
      page.on('pageerror', (error) => errors.push(error.message));
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/catalog');
      await expect(
        page.getByRole('link', { name: 'E2E studio', exact: true }),
      ).toBeVisible();
      await page.keyboard.press('Tab');
      await expect(
        page.getByRole('link', { name: 'Перейти к содержимому' }),
      ).toBeFocused();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: `test-results/catalog-${width}.png`,
        fullPage: true,
      });
      await page.getByRole('link', { name: 'E2E studio', exact: true }).click();
      await expect(
        page.getByRole('heading', {
          name: 'E2E studio',
          level: 1,
          exact: true,
        }),
      ).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      expect(errors).toEqual([]);
      await page.screenshot({
        path: `test-results/detail-${width}.png`,
        fullPage: true,
      });
    });
  }
});
