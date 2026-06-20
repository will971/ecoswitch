import { test as base } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

const istanbulUUID = () => Math.random().toString(36).substring(2, 15);

export const test = base.extend({
  page: async ({ page }, use) => {
    await use(page);

    // After each test, extract coverage
    try {
      const coverage = await page.evaluate(() => window.__coverage__);
      if (coverage) {
        const nycOutputDir = path.join(process.cwd(), '.nyc_output');
        if (!fs.existsSync(nycOutputDir)) {
          fs.mkdirSync(nycOutputDir, { recursive: true });
        }
        fs.writeFileSync(
          path.join(nycOutputDir, `playwright-${istanbulUUID()}.json`),
          JSON.stringify(coverage)
        );
      }
    } catch (e) {
      // Ignore errors if the page was closed during the test
    }
  }
});

export { expect } from '@playwright/test';
