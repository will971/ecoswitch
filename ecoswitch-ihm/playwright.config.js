import { defineConfig, devices } from '@playwright/test';

// PLAYWRIGHT_BASE_URL permet de viser un serveur deja lance (workspace
// Conductor, recette...). Sans elle, Playwright demarre lui-meme Vite sur 5173.
const externalBaseURL = process.env.PLAYWRIGHT_BASE_URL;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: externalBaseURL || 'http://localhost:5173',
    trace: 'on-first-retry',
  },

  /* Configure projects for major platforms and devices */
  projects: [
    {
      name: 'Desktop Chrome',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'Desktop Safari',
      use: { ...devices['Desktop Safari'] },
    },
    /* Mobile profiles (Touch emulation + Viewports + User-Agent) */
    {
      name: 'Mobile Safari (iPhone 12)',
      use: { ...devices['iPhone 12'] },
    },
    /* Tablet profiles */
    {
      name: 'Tablet (iPad Mini)',
      use: { ...devices['iPad Mini'] },
    },
  ],

  /* Run local dev server before starting the tests */
  webServer: externalBaseURL ? undefined : {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 30 * 1000,
  },
});
