import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import path from 'node:path';

const root = path.resolve('..');
const run = process.env.COMPASS_E2E_DIR;
if (!run) throw new Error('Use the isolated scripts/test-e2e.sh runner.');
const profile = (name: string) =>
  JSON.parse(
    readFileSync(
      path.join(root, `data/fixtures/profiles/${name}.json`),
      'utf8',
    ),
  );
const command = (...args: string[]) =>
  execFileSync(
    'java',
    [
      '-jar',
      path.join(root, 'backend/target/ai-compass-0.1.0.jar'),
      '--spring.main.banner-mode=off',
      '--logging.level.root=ERROR',
      ...args,
    ],
    { encoding: 'utf8', env: process.env, timeout: 30000 },
  );

test.describe
  .serial('recommendation preview with synthetic catalog and real API', () => {
  test.setTimeout(60000);
  test.beforeAll(() => {
    const manifest = JSON.parse(
      readFileSync(
        path.join(root, 'data/fixtures/recommendation-demo.json'),
        'utf8',
      ),
    );
    manifest.versionId = randomUUID();
    const file = path.join(run!, 'recommendation.json');
    writeFileSync(file, JSON.stringify(manifest), { mode: 0o600 });
    const imported = command('--catalog-command=import', `--file=${file}`);
    const hash = imported.match(/hash=([a-f0-9]{64})/)?.[1];
    if (!hash) throw new Error('Missing imported hash');
    command(
      '--catalog-command=review',
      `--version=${manifest.versionId}`,
      `--hash=${hash}`,
      '--actor=e2e',
    );
    const current = command('--catalog-command=current');
    const revision = current.match(/revision=(\d+)/)?.[1];
    if (!revision) throw new Error('Missing publication revision');
    command(
      '--catalog-command=publish',
      `--version=${manifest.versionId}`,
      `--expected-revision=${revision}`,
      '--actor=e2e',
      '--reason=Synthetic P04 acceptance',
    );
  });
  test('CSRF is mandatory and student preview returns free evidence-backed bundle', async ({
    request,
  }) => {
    const data = profile('student');
    expect(
      (
        await request.post('/api/v1/recommendations/preview', { data })
      ).status(),
    ).toBe(403);
    const csrf = await (await request.get('/api/v1/auth/csrf')).json();
    const response = await request.post('/api/v1/recommendations/preview', {
      data,
      headers: { [csrf.headerName]: csrf.token },
    });
    expect(response.status()).toBe(200);
    const result = await response.json();
    expect(result.status).toBe('MATCH');
    expect(result.bundles[0].costs.recurringMonthly).toBe('0');
    expect(
      result.bundles[0].items
        .map((item: { productId: string }) => item.productId)
        .sort(),
    ).toEqual(['learner', 'slides']);
    expect(result.evidence.length).toBeGreaterThan(0);
  });
  test('browser same-origin preview produces editor bundle without a P05 form', async ({
    page,
  }) => {
    await page.goto('/');
    const result = await page.evaluate(async (input) => {
      const csrf = await (await fetch('/api/v1/auth/csrf')).json();
      const response = await fetch('/api/v1/recommendations/preview', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token,
        },
        body: JSON.stringify(input),
      });
      return { status: response.status, body: await response.json() };
    }, profile('editor'));
    expect(result.status).toBe(200);
    expect(result.body.bundles[0].items[0].productId).toBe('creator');
    expect(result.body.bundles[0].costs.recurringMonthly).toBe('15');
  });
  test('unknown mandatory function yields no match and privileged input is rejected', async ({
    request,
  }) => {
    const data = profile('student');
    data.needs[0].id = 'unrepresented-function';
    const csrf = await (await request.get('/api/v1/auth/csrf')).json();
    const headers = { [csrf.headerName]: csrf.token };
    const result = await (
      await request.post('/api/v1/recommendations/preview', { data, headers })
    ).json();
    expect(result.bundles).toEqual([]);
    expect(
      result.unmetRequirements.some(
        (r: { state: string }) => r.state === 'UNKNOWN',
      ),
    ).toBe(true);
    const invalid = await request.post('/api/v1/recommendations/preview', {
      data: { ...data, premium: true },
      headers,
    });
    expect(invalid.status()).toBe(400);
    expect(invalid.headers()['content-type']).toContain(
      'application/problem+json',
    );
  });
});
