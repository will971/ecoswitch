import { test, expect } from './fixtures.js';

/**
 * Recherche facultative par plaque sur l'ecran « carburant » du wizard.
 *
 * L'appel a l'API est intercepte dans le navigateur : on teste ici le
 * comportement de l'interface, sans declencher la requete sortante vers
 * Oscaro que ferait l'API (testee a part, avec un fetch simule).
 */
async function goToFuelScreen(page) {
  await page.goto('/');
  await page.locator('.option-card-touch:has-text("Ma voiture personnelle")').click();
  await page.locator('.option-card-touch:has-text("Propriétaire (payé comptant)")').click();
  await expect(page.locator('#plate-input')).toBeVisible();
}

test.describe('Recherche par plaque', () => {
  test('preremplit le vehicule et passe a la consommation', async ({ page }) => {
    await page.route('**/api/v1/immatriculation/**', (route) =>
      route.fulfill({
        json: {
          name: 'Peugeot 208 II 1.2 PureTech 100 (2020)',
          fuelType: 'PETROL',
          consumption: 5.2,
          annualMileage: 15000,
          maintenanceCost: 380,
          resaleValue: 11000,
          source: 'LOCAL_FALLBACK',
        },
      }),
    );
    await goToFuelScreen(page);
    await page.locator('#plate-input').fill('ab-123-cd');
    await page.locator('button:has-text("Rechercher")').click();

    // Ecran consommation, avec la valeur de la plaque et le nom prerempli.
    await expect(page.locator('button:has-text("Continuer (5.2 L/100)")')).toBeVisible();
    await expect(page.locator('input[placeholder^="ex: Mégane"]')).toHaveValue('Peugeot 208 II 1.2 PureTech 100 (2020)');
  });

  test('plaque inconnue : message discret, saisie manuelle intacte', async ({ page }) => {
    await page.route('**/api/v1/immatriculation/**', (route) =>
      route.fulfill({ status: 404, json: { error: 'Plaque introuvable' } }),
    );
    await goToFuelScreen(page);
    await page.locator('#plate-input').fill('ZZ-999-ZZ');
    await page.locator('button:has-text("Rechercher")').click();

    await expect(page.locator('[role="status"]')).toContainText('Plaque non reconnue');
    // On reste sur le choix du carburant, qui fonctionne normalement.
    await page.locator('.option-card-touch:has-text("Essence (SP95 / E10)")').click();
    await expect(page.locator('button:has-text("Continuer (")')).toBeVisible();
  });
});
