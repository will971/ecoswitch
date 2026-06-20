import { test, expect } from './fixtures.js';

test.describe('Profitability Simulation & Authentication', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to homepage
    await page.goto('/');
  });

  test('should execute a full direct simulation successfully', async ({ page }) => {
    // Locators for the two vehicle form blocks
    const currentBlock = page.locator('.vehicle-form-block:has(h4:has-text("Véhicule Actuel"))');
    const targetBlock = page.locator('.vehicle-form-block:has(h4:has-text("Nouveau Véhicule"))');

    // Fill Current Vehicle details
    await currentBlock.locator('input[placeholder*="Peugeot 208"]').fill('Peugeot 207');
    await currentBlock.locator('select').filter({ hasText: 'Sélectionnez un carburant' }).selectOption('DIESEL');
    await currentBlock.locator('input[type="number"]').first().fill('5.2'); // Consommation
    await currentBlock.locator('input[type="number"]').nth(1).fill('1800');  // Reprise actuelle
    await currentBlock.locator('input[type="number"]').nth(2).fill('15000'); // Kilométrage annuel

    // Fill Target Vehicle details
    await targetBlock.locator('input[placeholder*="Tesla Model 3"]').fill('Renault Zoe');
    await targetBlock.locator('select').filter({ hasText: 'Sélectionnez un carburant' }).selectOption('ELECTRIC');
    await targetBlock.locator('input[type="number"]').first().fill('17.2'); // Consommation
    await targetBlock.locator('input[type="number"]').nth(1).fill('24000'); // Prix d'achat

    // Click on calculate button
    const calculateBtn = page.locator('button:has-text("Calculer la rentabilité")');
    await calculateBtn.click();

    // Verify loading screen or results are loaded
    const resultsContainer = page.locator('.apple-results-dashboard');
    await expect(resultsContainer).toBeVisible({ timeout: 10000 });

    // Assert that the results block contains key labels (TCO, gains)
    await expect(page.locator('body')).toContainText(/Coût Net de Transition/i);
    await expect(page.locator('body')).toContainText(/Analyse Financière Mensuelle/i);

    // Verify clicking "Modifier la saisie" goes back to form
    const editBtn = page.locator('button:has-text("Modifier la saisie")');
    if (await editBtn.isVisible()) {
      await editBtn.click();
      await expect(calculateBtn).toBeVisible();
    }
  });

  test('should display client authentication modal', async ({ page, isMobile }) => {
    // Locate the customer space login button
    let clientSpaceBtn;
    if (isMobile) {
      // Bottom navigation doesn't have login directly, but let's check
      // App.vue has Client Space button in sidebar, which is hidden on mobile.
      // On mobile, the button is not present unless they click something else.
      // So let's skip auth click on mobile or mock it.
      test.skip(isMobile, 'Skip desktop auth button test on mobile device viewports');
    } else {
      clientSpaceBtn = page.locator('.user-auth-section >> text=Espace Client');
    }

    await clientSpaceBtn.click();

    // Verify authentication modal appears
    const authModal = page.locator('.auth-modal-card');
    await expect(authModal).toBeVisible();

    // Check title in modal
    await expect(authModal.locator('h3')).toContainText(/Espace Client Connexion/i);

    // Fill dummy email and password
    await authModal.locator('input[type="email"]').fill('test@saas.com');
    await authModal.locator('input[type="password"]').fill('secretpassword');

    // Close auth modal
    const closeBtn = authModal.locator('button[aria-label="Fermer la fenêtre"]');
    await closeBtn.click();
    await expect(authModal).not.toBeVisible();
  });

});
