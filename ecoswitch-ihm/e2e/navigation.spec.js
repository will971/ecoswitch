import { test, expect } from './fixtures.js';

test.describe('General Navigation & Responsiveness', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to local frontend server
    await page.goto('/');
  });

  test('should load app with correct title and header', async ({ page }) => {
    await expect(page).toHaveTitle(/EcoSwitch/);
    const brandTitle = page.locator('.brand-title');
    await expect(brandTitle).toContainText('EcoSwitch');
  });

  test('should toggle theme between light and dark modes', async ({ page }) => {
    // Verify default theme is set (should be 'dark' or 'light')
    const html = page.locator('html');
    const initialTheme = await html.getAttribute('data-theme');
    expect(['light', 'dark']).toContain(initialTheme);

    // Click the theme toggle button (desktop sidebar)
    const themeBtn = page.locator('.theme-toggle-btn');
    await themeBtn.click();

    // Verify it toggled to the opposite theme
    const expectedTheme = initialTheme === 'light' ? 'dark' : 'light';
    await expect(html).toHaveAttribute('data-theme', expectedTheme);
  });

  test('should support tab switching in desktop viewport', async ({ page, isMobile }) => {
    test.skip(isMobile, 'Skip desktop navigation check on mobile device viewports');

    // Default tab should be the simulator ('direct')
    const directHeader = page.locator('h2.hero-title');
    await expect(directHeader).toContainText(/Simulateur de Rentabilité/);

    // Navigate to Comparateur
    const compareBtn = page.locator('.sidebar-nav >> text=Comparateur');
    await compareBtn.click();
    await expect(page.locator('h2')).toContainText(/Comparateur du Catalogue/i);

    // Navigate to Catalogue H2
    const catalogBtn = page.locator('.sidebar-nav >> text=Catalogue H2');
    await catalogBtn.click();
    await expect(page.locator('h2')).toContainText(/Catalogue des Véhicules/i);
  });

  test('should hide sidebar and show bottom nav on mobile layout', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Skip mobile layouts check on desktop viewports');

    // On mobile, the desktop sidebar navigation links should be hidden
    const sidebarNav = page.locator('aside.sidebar-left .sidebar-nav');
    await expect(sidebarNav).not.toBeVisible();

    // The mobile bottom nav should be visible
    const mobileBottomNav = page.locator('nav.mobile-bottom-nav');
    await expect(mobileBottomNav).toBeVisible();

    // Click the 2nd button (index 1) in the mobile nav (should map to Comparateur)
    const mobileCompareBtn = mobileBottomNav.locator('button').nth(1);
    await mobileCompareBtn.click();
    await expect(page.locator('h2')).toContainText(/Comparateur du Catalogue/i);

    // Click the 3rd button (index 2) in the mobile nav (should map to Catalogue H2)
    const mobileCatalogBtn = mobileBottomNav.locator('button').nth(2);
    await mobileCatalogBtn.click();
    await expect(page.locator('h2')).toContainText(/Catalogue des Véhicules/i);
  });

});
