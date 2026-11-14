import { test, expect, type Page } from '@playwright/test';
import { readFileSync, readdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
// Auth traces would record cookies and one-time links. Never retain them.
test.use({ trace: 'off', video: 'off' });
const run = process.env.COMPASS_E2E_DIR;
if (!run || process.env.DB_NAME !== 'compass_e2e')
  throw new Error('Only the isolated E2E database is allowed');
const password = 'Synthetic-test-password-973!';
let used = 30;
const watched = new WeakSet<Page>();
function watch(page: Page) {
  if (watched.has(page)) return;
  watched.add(page);
  page.on('request', (r) => {
    if (
      r.url().endsWith('/auth/csrf') ||
      r.url().endsWith('/recommendations/preview')
    )
      used++;
  });
}
function token(email: string, purpose: string) {
  for (const file of readdirSync(path.join(run!, 'mail'))) {
    const message = readFileSync(path.join(run!, 'mail', file), 'utf8');
    if (message.startsWith(`To: ${email}\nPurpose: ${purpose}\n`))
      return message.split('#token=')[1].trim();
  }
  throw new Error('Local message not found');
}
function email() {
  return `test-${randomUUID()}@example.invalid`;
}
async function registerVerify(
  page: Page,
  address: string,
  alreadyThere = false,
) {
  watch(page);
  if (!alreadyThere) await page.goto('/register');
  await page.getByLabel('Email', { exact: true }).fill(address);
  await page.getByLabel('Пароль', { exact: false }).fill(password);
  await page
    .getByRole('button', { name: 'Создать аккаунт', exact: true })
    .click();
  await expect(page.getByRole('status')).toContainText('Если адрес подходит');
  await page
    .getByRole('link', { name: 'Подтверждение email', exact: true })
    .click();
  await page.getByLabel('Код из письма').fill(token(address, 'VERIFY'));
  await page.getByRole('button', { name: 'Подтвердить email' }).click();
  await expect(page.getByRole('status')).toContainText('Email подтверждён');
}
async function login(page: Page, address: string, pw = password) {
  await page
    .getByRole('link', { name: 'Войти в аккаунт', exact: true })
    .click();
  await page.getByLabel('Email', { exact: true }).fill(address);
  await page.getByLabel('Пароль', { exact: false }).fill(pw);
  await page.getByRole('button', { name: 'Войти', exact: true }).click();
  await expect(page).toHaveURL(/\/(dashboard|results)$/);
}
async function request(
  page: Page,
  method: string,
  route: string,
  data?: unknown,
) {
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  used++;
  return page.request.fetch('/api/v1' + route, {
    method,
    data,
    headers: { [csrf.headerName]: csrf.token },
  });
}
async function guestPreview(page: Page) {
  await page.goto('/onboarding');
  await page.getByLabel('Разработка программ', { exact: true }).check();
  await page
    .getByLabel('Частота: Разработка программ', { exact: true })
    .selectOption('WEEKLY');
  await page.getByRole('button', { name: 'Продолжить' }).click();
  await page.getByLabel('Страна покупки (код)').fill('KZ');
  await page.getByLabel('Язык интерфейса (код)').fill('ru');
  await page.getByLabel('Способ покупки').fill('WEB');
  await page.getByRole('button', { name: 'Продолжить' }).click();
  await page.getByLabel('Валюта бюджета').fill('USD');
  await page.getByLabel('Месячный бюджет на весь AI').fill('20');
  await page.getByLabel('Доступная предоплата').fill('30');
  await page.getByRole('button', { name: 'Подобрать мой AI-набор' }).click();
  await expect(page).toHaveURL(/\/results$/);
}
test.describe('P06 isolated accounts and personal data', () => {
  test.setTimeout(120000);
  test.beforeEach(async ({ page }) => {
    watch(page);
    if (used >= 12) {
      await new Promise((r) => setTimeout(r, 61000));
      used = 0;
    }
  });
  test('register verify login logout and persistent revisit', async ({
    page,
  }) => {
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    await page.getByRole('link', { name: 'Аккаунт', exact: true }).click();
    await expect(page.getByRole('heading', { name: address })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading', { name: address })).toBeVisible();
    await page.getByRole('button', { name: 'Выйти', exact: true }).click();
    await page.getByRole('link', { name: 'Аккаунт', exact: true }).click();
    await expect(page.getByText(/Для личных данных нужно войти/)).toBeVisible();
  });
  test('guest result survives registration and saves only after consent; rerun and Free limit', async ({
    page,
  }) => {
    const writes: string[] = [];
    page.on('request', (r) => {
      if (
        r.method() === 'POST' &&
        /\/api\/v1\/(profiles|recommendations|saved-stacks)$/.test(
          new URL(r.url()).pathname,
        )
      )
        writes.push(r.url());
    });
    await guestPreview(page);
    expect(writes).toHaveLength(0);
    await page
      .getByRole('link', { name: 'Создать аккаунт', exact: true })
      .click();
    const address = email();
    await registerVerify(page, address, true);
    await login(page, address);
    await expect(page).toHaveURL(/\/results$/);
    expect(writes).toHaveLength(0);
    await expect(
      page.getByRole('button', { name: 'Сохранить результат', exact: true }),
    ).toBeDisabled();
    await page
      .getByLabel('Подтверждаю сохранение указанных параметров в этот аккаунт')
      .check();
    await page
      .getByRole('button', { name: 'Сохранить профиль', exact: true })
      .click();
    await expect(
      page.getByRole('status').filter({ hasText: 'Профиль сохранён' }),
    ).toBeVisible();
    await page.getByLabel('Выбранный AI-набор').selectOption({ index: 1 });
    await page
      .getByRole('button', { name: 'Сохранить результат', exact: true })
      .click();
    await expect(
      page
        .getByRole('status')
        .filter({ hasText: 'Исторический результат сохранён' }),
    ).toBeVisible();
    await page
      .getByRole('link', { name: 'Мои сохранения', exact: true })
      .first()
      .click();
    await expect(
      page.getByText('Мой AI-профиль', { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Открыть сохранённый результат' }),
    ).toHaveCount(1);
    await expect(
      page.getByRole('button', { name: 'Удалить выбранный набор' }),
    ).toHaveCount(1);
    const existing = await (await page.request.get('/api/v1/profiles')).json();
    expect(
      (
        await request(page, 'POST', '/profiles', {
          name: 'Bypass',
          profile: existing[0].profile,
          revision: 0,
        })
      ).status(),
    ).toBe(409);
    await page
      .getByRole('button', { name: 'Открыть сохранённый результат' })
      .click();
    await expect(page.getByText('Цена не подтверждена').first()).toBeVisible();
    await page
      .getByRole('link', { name: 'Мои сохранения', exact: true })
      .first()
      .click();
    await page
      .getByRole('button', { name: 'Открыть профиль и повторить подбор' })
      .click();
    await page.getByRole('button', { name: '3. Бюджет и подписки' }).click();
    await expect(page.getByLabel('Месячный бюджет на весь AI')).toHaveValue(
      '20',
    );
    await page.getByRole('button', { name: 'Подобрать мой AI-набор' }).click();
    await expect(page).toHaveURL(/\/results$/);
  });
  test('password reset revokes existing session and accepts only the new password', async ({
    page,
  }) => {
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    await page.getByRole('link', { name: 'Аккаунт', exact: true }).click();
    await page
      .getByRole('link', { name: 'Сменить пароль через подтверждение email' })
      .click();
    await page.getByLabel('Email', { exact: true }).fill(address);
    await page.getByRole('button', { name: 'Восстановить пароль' }).click();
    await expect(page.getByRole('status')).toContainText('инструкция доступна');
    await page.goto('/reset-password');
    await page.getByLabel('Код из письма').fill(token(address, 'RESET'));
    await page.getByLabel('Пароль', { exact: false }).fill(password + 'new');
    await page.getByRole('button', { name: 'Новый пароль' }).click();
    await expect(page.getByRole('status')).toContainText('Пароль изменён');
    expect((await page.request.get('/api/v1/me')).status()).toBe(401);
    await login(page, address, password + 'new');
  });
  test('A cannot read update or delete B profile by direct API request', async ({
    page,
    browser,
  }) => {
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    const otherContext = await browser.newContext({
      baseURL: new URL(page.url()).origin,
    });
    const other = await otherContext.newPage();
    try {
      const otherAddress = email();
      await registerVerify(other, otherAddress);
      await login(other, otherAddress);
      const sample = JSON.parse(
        readFileSync(
          path.resolve('../data/fixtures/profiles/student.json'),
          'utf8',
        ),
      );
      sample.asOf = null;
      const created = await request(other, 'POST', '/profiles', {
        name: 'Private B',
        profile: sample,
        revision: 0,
      });
      expect(created.status()).toBe(201);
      const data = await created.json();
      expect(
        (await page.request.get('/api/v1/profiles/' + data.id)).status(),
      ).toBe(404);
      expect(
        (
          await request(page, 'PATCH', '/profiles/' + data.id, {
            name: 'Attack',
            profile: sample,
            revision: 1,
          })
        ).status(),
      ).toBe(404);
      expect(
        (await request(page, 'DELETE', '/profiles/' + data.id)).status(),
      ).toBe(404);
      expect(
        (await other.request.get('/api/v1/profiles/' + data.id)).status(),
      ).toBe(200);
    } finally {
      await otherContext.close();
    }
  });
  test('expired database session removes private UI and asks to sign in', async ({
    page,
  }) => {
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    // Only this harness-owned PostgreSQL; no workspace account or catalog is touched.
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
        `UPDATE compass.spring_session SET last_access_time=0,expiry_time=0 WHERE principal_name=(SELECT id::text FROM compass.user_account WHERE email='${address}')`,
      ],
      { stdio: 'pipe' },
    );
    await page.reload();
    await expect(page.getByText(/Для личных данных нужно войти/)).toBeVisible();
    expect((await page.request.get('/api/v1/me')).status()).toBe(401);
  });
  test('export and confirmed deletion preserve public catalog and guest preview', async ({
    page,
  }) => {
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    await page.getByRole('link', { name: 'Аккаунт', exact: true }).click();
    const download = page.waitForEvent('download');
    await page
      .getByRole('button', { name: 'Экспортировать мои данные' })
      .click();
    const file = await download;
    const stream = await file.createReadStream();
    const chunks: Buffer[] = [];
    for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
    const content = Buffer.concat(chunks).toString();
    const data = JSON.parse(content);
    expect(data.account.email).toBe(address);
    expect(content.includes('password')).toBe(false);
    expect(content.includes('token')).toBe(false);
    await page.getByLabel('Текущий пароль').fill(password);
    await page
      .getByLabel('Подтверждаю удаление аккаунта и личных данных')
      .check();
    await page
      .getByRole('button', { name: 'Удалить аккаунт безвозвратно' })
      .click();
    await expect(page).toHaveURL(/\/$/);
    expect((await page.request.get('/api/v1/me')).status()).toBe(401);
    await guestPreview(page);
  });
  test('account and dashboard are accessible without overflow at 360/768/1440', async ({
    page,
  }) => {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    const address = email();
    await registerVerify(page, address);
    await login(page, address);
    for (const width of [360, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      await page.getByRole('link', { name: 'Аккаунт', exact: true }).click();
      await expect(page.getByRole('heading', { name: address })).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      await page.getByRole('button', { name: 'Выйти', exact: true }).focus();
      await expect(
        page.getByRole('button', { name: 'Выйти', exact: true }),
      ).toBeFocused();
      await page.screenshot({
        path: path.join(run!, `p06-account-${width}.png`),
        fullPage: true,
      });
      await page
        .getByRole('link', { name: 'Мои сохранения', exact: true })
        .first()
        .click();
      await expect(page.getByText(/Пока нет профиля/)).toBeVisible();
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: path.join(run!, `p06-dashboard-${width}.png`),
        fullPage: true,
      });
    }
    expect(errors).toEqual([]);
    const log = readFileSync(path.join(run!, 'backend.log'), 'utf8');
    expect(log.includes(password)).toBe(false);
    expect(log.includes(token(address, 'VERIFY'))).toBe(false);
    for (const cookie of await page.context().cookies())
      expect(log.includes(cookie.value)).toBe(false);
  });
});
