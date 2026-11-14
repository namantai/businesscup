import { expect, test } from '@playwright/test';

for (const width of [360, 768, 1440]) {
  test(`local scaffold and backend readiness at ${width}px`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: 900 });
    const errors: string[] = [];
    page.on('pageerror', (error) => errors.push(error.message));
    page.on('console', (message) => {
      if (message.type() === 'error') errors.push(message.text());
    });
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText(
      'Ваш набор AI',
    );
    await expect(page.getByRole('status')).toHaveText('Сервис доступен');
    await page.getByRole('button').click();
    await expect(page.getByRole('status')).toHaveText('Сервис доступен');
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
    expect(errors).toEqual([]);
    await page.screenshot({
      path: `test-results/scaffold-${width}.png`,
      fullPage: true,
    });
  });
}
