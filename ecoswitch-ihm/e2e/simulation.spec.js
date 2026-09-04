import { test, expect } from './fixtures.js';

test.describe('Profitability Simulation & Authentication', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to homepage
    await page.goto('/');
  });

  test('should execute a full direct simulation successfully', async ({ page }) => {
    // Step 1: Transport mode - select personal car
    await page.locator('.option-card-touch:has-text("Ma voiture personnelle")').click();

    // Step 2: Vehicle ownership status - cash
    await page.locator('.option-card-touch:has-text("Propriétaire (payé comptant)")').click();

    // Step 3: Current fuel type - petrol
    await page.locator('.option-card-touch:has-text("Essence (SP95 / E10)")').click();

    // Step 4: Current model - pick a popular car (e.g. Peugeot 208)
    await page.locator('button.quick-car-btn:has-text("Peugeot 208")').click();

    // Step 5: Annual mileage - continue with default
    await page.locator('button:has-text("Continuer")').click();

    // Step 6: Maintenance - confirm default
    await page.locator('button:has-text("Conserver l\'estimation moyenne constructeur")').click();

    // Step 7: Vehicle departure - resale
    await page.locator('.option-card-touch:has-text("Revente d\'occasion")').click();

    // Step 8: Charging location - individual home
    await page.locator('.option-card-touch:has-text("Maison individuelle")').click();

    // Step 9: Tax income tier - standard
    await page.locator('.option-card-touch:has-text("Plus de 15 400 €")').click();

    // Step 10: Preferred target format - city car
    await page.locator('.option-card-touch:has-text("Citadine agile")').click();

    // Step 11: Target monthly budget - submit
    await page.locator('button:has-text("Calculer mes économies")').click();

    // Step 13: Final auth step - proceed without account
    await page.locator('button:has-text("Découvrir mes résultats sans compte")').click();

    // Verify loading screen or results are loaded
    const resultsContainer = page.locator('.results-dashboard');
    await expect(resultsContainer).toBeVisible({ timeout: 10000 });

    // Assert that the results block contains key labels
    await expect(page.locator('body')).toContainText(/Impact sur votre Budget/i);
    await expect(page.locator('body')).toContainText(/Modifier la saisie/i);

    // Verify clicking "Modifier la saisie" goes back to form
    const editBtn = page.locator('button:has-text("Modifier la saisie")');
    if (await editBtn.isVisible()) {
      await editBtn.click();
      await expect(page.locator('.step-wizard-container')).toBeVisible();
    }

    // Verify that express and expert mode elements are not visible
    await expect(page.locator('.segmented-control button:has-text("Express")')).not.toBeVisible();
    await expect(page.locator('.segmented-control button:has-text("Expert")')).not.toBeVisible();
    await expect(page.locator('.btn-switch-expert-subtle')).not.toBeVisible();
    await expect(page.locator('.btn-footer-mode')).not.toBeVisible();
  });

  test('should display client authentication modal', async ({ page, isMobile }) => {
    if (isMobile) {
      // On mobile, open drawer from bottom nav first
      const menuBtn = page.locator('nav.mobile-bottom-nav button.bottom-nav-item:has-text("Menu")');
      await menuBtn.click();
    }

    const clientSpaceBtn = page.locator('button:has-text("Espace Client")').first();
    await clientSpaceBtn.click();

    // Verify authentication modal appears
    const authModal = page.locator('.auth-modal-card');
    await expect(authModal).toBeVisible();

    // Check title in modal
    await expect(authModal.locator('h3')).toContainText(/Connexion à votre compte/i);

    // Fill dummy email and password
    await authModal.locator('input[type="email"]').fill('test@saas.com');
    await authModal.locator('input[type="password"]').fill('secretpassword');

    // Close auth modal
    const closeBtn = authModal.locator('button[aria-label="Fermer la fenêtre"]');
    await closeBtn.click();
    await expect(authModal).not.toBeVisible();
  });

});
