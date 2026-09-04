import { test, expect } from './fixtures.js';

test.describe('General Navigation & Responsiveness', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to local frontend server
    await page.goto('/');
  });

  test('should load app with correct title and header', async ({ page }) => {
    await expect(page).toHaveTitle(/EcoSwitch/);
    const brandTitle = page.locator('.brand-name').first();
    await expect(brandTitle).toContainText('EcoSwitch');
  });

  test('should toggle theme between light and dark modes', async ({ page }) => {
    // Verify default theme is set (should be 'dark' or 'light')
    const html = page.locator('html');
    const initialTheme = await html.getAttribute('data-theme');
    expect(['light', 'dark']).toContain(initialTheme);

    // Click the visible theme toggle button
    const themeBtn = page.locator('.btn-theme-toggle:visible');
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
    await expect(page.locator('.hero-luxury-title')).toContainText(/Comparateur/i);

    // Navigate to Catalogue
    const catalogBtn = page.locator('.sidebar-nav >> text=Catalogue');
    await catalogBtn.click();
    await expect(page.locator('.manager-main-title')).toContainText(/Catalogue/i);
  });

  test('should hide sidebar and show bottom nav on mobile layout', async ({ page, isMobile }) => {
    test.skip(!isMobile, 'Skip mobile layouts check on desktop viewports');

    // On mobile, the desktop sidebar navigation links should be hidden
    const sidebarNav = page.locator('aside.app-sidebar .sidebar-nav');
    await expect(sidebarNav).not.toBeVisible();

    // The mobile bottom nav should be visible
    const mobileBottomNav = page.locator('nav.mobile-bottom-nav');
    await expect(mobileBottomNav).toBeVisible();

    // Click the 2nd item (index 1) in the mobile nav (should map to Comparateur)
    const mobileCompareBtn = mobileBottomNav.locator('.bottom-nav-item').nth(1);
    await mobileCompareBtn.click();
    await expect(page.locator('.hero-luxury-title')).toContainText(/Comparateur/i);

    // Click the 3rd item (index 2) in the mobile nav (should map to Catalogue)
    const mobileCatalogBtn = mobileBottomNav.locator('.bottom-nav-item').nth(2);
    await mobileCatalogBtn.click();
    await expect(page.locator('.manager-main-title')).toContainText(/Catalogue/i);
  });

});
