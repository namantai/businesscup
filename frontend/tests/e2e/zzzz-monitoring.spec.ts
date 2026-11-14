import { test, expect, type Page } from '@playwright/test';
import { readFileSync, readdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import path from 'node:path';

test.use({ trace: 'off', video: 'off' });
const run = process.env.COMPASS_E2E_DIR;
if (!run || process.env.DB_NAME !== 'compass_e2e')
  throw new Error('Isolated monitoring database required');
const root = path.resolve('..');
const password = 'Synthetic-monitor-only-239!';
async function send(page: Page, method: string, route: string, data?: unknown) {
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  return page.request.fetch('/api/v1' + route, {
    method,
    data,
    headers: { [csrf.headerName]: csrf.token },
  });
}
async function register(page: Page, admin = false) {
  const email = randomUUID() + '@example.invalid';
  expect(
    (await send(page, 'POST', '/auth/register', { email, password })).status(),
  ).toBe(202);
  let token = '';
  for (const file of readdirSync(path.join(run!, 'mail'))) {
    const text = readFileSync(path.join(run!, 'mail', file), 'utf8');
    if (text.startsWith(`To: ${email}\nPurpose: VERIFY\n`))
      token = text.split('#token=')[1].trim();
  }
  expect(!!token).toBe(true);
  expect((await send(page, 'POST', '/auth/verify', { token })).status()).toBe(
    200,
  );
  if (admin) {
    // Own synthetic fixture only; role is assigned server-side before login, never via public JSON.
    execFileSync(
      'docker',
      [
        'exec',
        'ai-compass-' + path.basename(run!),
        'psql',
        '-U',
        'compass_e2e',
        '-d',
        'compass_e2e',
        '-c',
        `UPDATE compass.user_account SET role='ADMIN' WHERE email='${email}'`,
      ],
      { stdio: 'pipe' },
    );
  }
  expect(
    (await send(page, 'POST', '/auth/login', { email, password })).status(),
  ).toBe(200);
}
function fixture() {
  const m = JSON.parse(
    readFileSync(
      path.join(root, 'data/fixtures/recommendation-demo.json'),
      'utf8',
    ),
  );
  m.versionId = randomUUID();
  return m;
}
async function initialPublish(page: Page, m: unknown) {
  let response = await send(page, 'POST', '/admin/catalog/drafts', {
    manifest: JSON.stringify(m),
    revision: 0,
  });
  expect(response.status()).toBe(200);
  let d = await response.json();
  for (const action of ['VALIDATE', 'REVIEW', 'PUBLISH']) {
    const active = await (
      await page.request.get('/api/v1/admin/catalog/active')
    ).json();
    response = await send(
      page,
      'POST',
      `/admin/catalog/drafts/${d.id}/actions`,
      { action, revision: d.revision, activeRevision: active.revision },
    );
    expect(response.status()).toBe(200);
    d = await response.json();
  }
}
async function saveStack(page: Page, editor: boolean) {
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
  const preview = await send(page, 'POST', '/recommendations/preview', profile);
  expect(preview.status()).toBe(200);
  const r = await preview.json();
  const saved = await send(page, 'POST', '/recommendations', {
    profile,
    catalogVersion: r.catalogVersion,
    asOf: r.asOf,
    engineVersion: r.engineVersion,
    policyVersion: r.policyVersion,
  });
  expect(saved.status()).toBe(201);
  const snapshot = await saved.json();
  const stack = await send(page, 'POST', '/saved-stacks', {
    snapshotId: snapshot.id,
    bundleId: snapshot.result.bundles[0].id,
  });
  expect(stack.status()).toBe(201);
  const watched = await send(page, 'POST', '/monitoring/watches', {
    stackId: (await stack.json()).id,
    emailOptIn: false,
    consent: true,
  });
  expect(watched.status()).toBe(200);
}
test('admin publishes semantic change; only impacted owner receives one real notification', async ({
  page,
  browser,
}) => {
  test.setTimeout(300000);
  await new Promise((r) => setTimeout(r, 61000));
  await register(page, true);
  const manifest = fixture();
  await initialPublish(page, manifest);
  const aContext = await browser.newContext({
    baseURL: `http://127.0.0.1:${process.env.FRONTEND_PORT}`,
  });
  const bContext = await browser.newContext({
    baseURL: `http://127.0.0.1:${process.env.FRONTEND_PORT}`,
  });
  try {
    const a = await aContext.newPage(),
      b = await bContext.newPage();
    await register(a);
    await register(b);
    for (const participant of [a, b]) {
      const account = await (
        await participant.request.get('/api/v1/me')
      ).json();
      expect(
        (
          await send(page, 'POST', '/admin/entitlements/' + account.id, {
            requestId: randomUUID(),
            action: 'GRANT',
            days: 7,
            expectedRevision: 0,
          })
        ).status(),
      ).toBe(200);
    }
    await saveStack(a, true);
    await saveStack(b, false);
    expect((await b.request.get('/api/v1/admin/catalog/drafts')).status()).toBe(
      403,
    );
    await a.goto('/notifications');
    await expect(a.getByText('Существенных изменений пока нет.')).toBeVisible();
    await page.goto('/admin/catalog');
    await expect(
      page.getByRole('heading', { name: 'Управление каталогом' }),
    ).toBeVisible();
    manifest.versionId = randomUUID();
    const offer = manifest.priceOffers.find(
      (o: { id: string }) => o.id === 'creator-offer',
    );
    offer.amount = '18';
    offer.upfrontAmount = '18';
    await page.getByLabel('Manifest JSON').fill(JSON.stringify(manifest));
    await page
      .getByRole('button', { name: 'Сохранить черновик', exact: true })
      .click();
    await expect(
      page.getByRole('button', { name: 'Проверить каталог', exact: true }),
    ).toBeEnabled();
    await page
      .getByRole('button', { name: 'Проверить каталог', exact: true })
      .click();
    await expect(
      page.getByRole('button', {
        name: 'Одобрить проверенный снимок',
        exact: true,
      }),
    ).toBeEnabled();
    await page
      .getByRole('button', { name: 'Одобрить проверенный снимок', exact: true })
      .click();
    await expect(
      page.getByRole('button', { name: 'Опубликовать', exact: true }),
    ).toBeEnabled();
    await page
      .getByRole('button', { name: 'Опубликовать', exact: true })
      .click();
    await expect(page.getByText(/Состояние: PUBLISHED/)).toBeVisible();
    await expect
      .poll(
        async () =>
          (await (await a.request.get('/api/v1/monitoring/notices')).json())
            .length,
        { timeout: 150000, intervals: [3000] },
      )
      .toBe(1);
    expect(
      (await (await b.request.get('/api/v1/monitoring/notices')).json()).length,
    ).toBe(0);
    await a.getByRole('button', { name: 'Обновить уведомления' }).click();
    await expect(
      a.getByRole('heading', { name: 'Было → стало' }),
    ).toBeVisible();
    await expect(
      a.getByText('15 → 18', { exact: false }).first(),
    ).toBeVisible();
    for (const width of [360, 768, 1440]) {
      await a.setViewportSize({ width, height: 1000 });
      expect(
        await a.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      await a.screenshot({
        path: path.join(run!, `p08-inbox-${width}.png`),
        fullPage: true,
      });
      await page.setViewportSize({ width, height: 1000 });
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: path.join(run!, `p08-admin-${width}.png`),
        fullPage: true,
      });
    }
    await a.getByRole('button', { name: 'Отметить прочитанным' }).click();
    await expect(
      a.getByRole('button', { name: 'Прочитано', exact: true }),
    ).toBeDisabled();
    await a
      .getByRole('button', {
        name: 'Отметить применение (со слов пользователя)',
      })
      .click();
    await expect(
      a.getByText(/покупка и экономия не подтверждены/),
    ).toBeVisible();
    expect(
      (await (await a.request.get('/api/v1/monitoring/notices')).json()).length,
    ).toBe(1);
  } finally {
    await aContext.close();
    await bContext.close();
  }
});

test('beta grants are server enforced, expire or revoke without a payment, and remain owned', async ({
  page,
  browser,
}) => {
  test.setTimeout(180000);
  await new Promise((r) => setTimeout(r, 61000));
  await register(page, true);
  const userContext = await browser.newContext({
    baseURL: `http://127.0.0.1:${process.env.FRONTEND_PORT}`,
  });
  try {
    const user = await userContext.newPage();
    await register(user);
    const account = await (await user.request.get('/api/v1/me')).json();
    await user.goto('/pricing?success=true&plan=PREMIUM');
    await expect(
      user.getByRole('heading', { name: 'Free и Premium — локальная beta' }),
    ).toBeVisible();
    expect(
      (await (await user.request.get('/api/v1/me/entitlements')).json()).plan,
    ).toBe('FREE');
    expect(
      (
        await send(user, 'POST', '/admin/entitlements/' + account.id, {
          requestId: randomUUID(),
          action: 'GRANT',
          days: 7,
          expectedRevision: 0,
        })
      ).status(),
    ).toBe(403);
    await page.goto('/account/billing');
    await page.getByLabel('UUID аккаунта').fill(account.id);
    await page.getByRole('button', { name: 'Загрузить права' }).click();
    await expect(
      page.getByRole('button', { name: 'Выдать beta-доступ' }),
    ).toBeDisabled();
    await page.getByLabel('Подтверждаю изменение beta-доступа').check();
    await page.getByRole('button', { name: 'Выдать beta-доступ' }).click();
    await expect(
      page.getByRole('button', { name: 'Выдать beta-доступ' }),
    ).toBeDisabled();
    await expect
      .poll(
        async () =>
          (await (await user.request.get('/api/v1/me/entitlements')).json())
            .plan,
      )
      .toBe('PREMIUM');
    await user.goto('/account/billing');
    await expect(
      user.getByRole('heading', { name: 'PREMIUM', exact: true }),
    ).toBeVisible();
    await expect(user.getByText('Состояние beta: действует')).toBeVisible();
    for (const width of [360, 768, 1440]) {
      await user.setViewportSize({ width, height: 900 });
      await expect
        .poll(() =>
          user.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
          ),
        )
        .toBe(true);
      await user.screenshot({
        path: path.join(run!, `p10-billing-${width}.png`),
        fullPage: true,
      });
    }
    await user.getByRole('button', { name: 'Обновить доступ' }).focus();
    await expect(
      user.getByRole('button', { name: 'Обновить доступ' }),
    ).toBeFocused();
    await user.keyboard.press('Enter');
    await page.getByLabel('Подтверждаю изменение beta-доступа').check();
    await page.getByRole('button', { name: 'Отозвать beta-доступ' }).click();
    await expect
      .poll(
        async () =>
          (await (await user.request.get('/api/v1/me/entitlements')).json())
            .plan,
      )
      .toBe('FREE');
    await user.getByRole('button', { name: 'Обновить доступ' }).click();
    await expect(user.getByText('Состояние beta: отозван')).toBeVisible();
    expect(
      (await (await user.request.get('/api/v1/me/export')).json())
        .entitlementAudit,
    ).toHaveLength(2);
    expect((await send(user, 'POST', '/auth/logout')).status()).toBe(204);
    expect((await user.request.get('/api/v1/me/entitlements')).status()).toBe(
      401,
    );
  } finally {
    await userContext.close();
  }
});
