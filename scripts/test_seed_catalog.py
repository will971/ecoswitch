#!/usr/bin/env python3
"""
Unit tests for the EcoSwitch Catalog Seeder script (scripts/seed_catalog.py).
Tests environment resolution, URL configuration, SVG generator functions, and catalog data schema.
"""

import unittest
import xml.etree.ElementTree as ET
from seed_catalog import (
    ENV_CONFIGS,
    resolve_target_url,
    configure_api_endpoints,
    generate_model_svg,
    BRAND_LOGOS_SVG,
    CATALOG_DATA,
    API_BASE,
    API_UPLOAD_URL
)

class TestSeedCatalog(unittest.TestCase):

    def test_resolve_target_url_prod(self):
        """Vérifie que l'argument 'prod' et 'production' résolvent vers l'URL de production Railway."""
        self.assertEqual(resolve_target_url("prod"), "https://ecoswitch-api.up.railway.app")
        self.assertEqual(resolve_target_url("PROD"), "https://ecoswitch-api.up.railway.app")
        self.assertEqual(resolve_target_url("production"), "https://ecoswitch-api.up.railway.app")
        self.assertEqual(resolve_target_url("PRODUCTION"), "https://ecoswitch-api.up.railway.app")

    def test_resolve_target_url_local(self):
        """Vérifie que par défaut ou avec 'local', l'URL pointe vers localhost:8080."""
        self.assertEqual(resolve_target_url(), "http://localhost:8080")
        self.assertEqual(resolve_target_url("local"), "http://localhost:8080")
        self.assertEqual(resolve_target_url("LOCAL"), "http://localhost:8080")
        self.assertEqual(resolve_target_url(""), "http://localhost:8080")

    def test_resolve_target_url_custom(self):
        """Vérifie que les URLs personnalisées sont correctement normalisées."""
        self.assertEqual(resolve_target_url("https://my-custom-api.com/"), "https://my-custom-api.com")
        self.assertEqual(resolve_target_url("http://192.168.1.50:8080"), "http://192.168.1.50:8080")

    def test_configure_api_endpoints(self):
        """Vérifie que la reconfiguration dynamique des endpoints met à jour les constantes globales."""
        import seed_catalog
        configure_api_endpoints("https://ecoswitch-api.up.railway.app")
        self.assertEqual(seed_catalog.API_BASE, "https://ecoswitch-api.up.railway.app/api/v1/catalog")
        self.assertEqual(seed_catalog.API_UPLOAD_URL, "https://ecoswitch-api.up.railway.app/api/v1/uploads/image")

        # Rétablir la config locale
        configure_api_endpoints("http://localhost:8080")
        self.assertEqual(seed_catalog.API_BASE, "http://localhost:8080/api/v1/catalog")
        self.assertEqual(seed_catalog.API_UPLOAD_URL, "http://localhost:8080/api/v1/uploads/image")

    def test_brand_logos_svg_validity(self):
        """Vérifie que tous les logos de marques sont des SVG XML valides."""
        self.assertGreaterEqual(len(BRAND_LOGOS_SVG), 20)
        for brand_name, svg_code in BRAND_LOGOS_SVG.items():
            try:
                root = ET.fromstring(svg_code)
                self.assertEqual(root.tag, "{http://www.w3.org/2000/svg}svg")
            except Exception as e:
                self.fail(f"Invalid SVG XML for brand '{brand_name}': {e}")

    def test_model_svg_generation(self):
        """Vérifie que la génération des silhouettes de modèles produit du SVG valide."""
        svg_ev = generate_model_svg("Tesla", "Model Y", "SUV", is_ev=True)
        self.assertIn("Model Y", svg_ev)
        self.assertIn("ÉLEC", svg_ev)
        root_ev = ET.fromstring(svg_ev)
        self.assertEqual(root_ev.tag, "{http://www.w3.org/2000/svg}svg")

        svg_hybrid = generate_model_svg("Toyota", "Yaris Cross", "SUV", is_ev=False)
        self.assertIn("Yaris Cross", svg_hybrid)
        self.assertIn("HYBRIDE", svg_hybrid)
        root_hybrid = ET.fromstring(svg_hybrid)
        self.assertEqual(root_hybrid.tag, "{http://www.w3.org/2000/svg}svg")

    def test_catalog_data_structure(self):
        """Vérifie la complétude et l'intégrité du jeu de données du catalogue."""
        self.assertEqual(len(CATALOG_DATA), 20)
        total_models = 0
        total_variants = 0

        for b in CATALOG_DATA:
            self.assertIn("brand", b)
            self.assertIn("models", b)
            self.assertGreaterEqual(len(b["models"]), 2)

            for m in b["models"]:
                total_models += 1
                self.assertIn("name", m)
                self.assertIn("motorisations", m)
                self.assertIn("finitions", m)
                self.assertIn("variants", m)

                for mot in m["motorisations"]:
                    self.assertIn("fuelType", mot)
                    self.assertIn("consumptionWltp", mot)
                    self.assertGreater(mot["consumptionWltp"], 0)

                for var in m["variants"]:
                    total_variants += 1
                    self.assertIn("finition", var)
                    self.assertIn("motorisation", var)
                    self.assertIn("price", var)
                    self.assertGreater(var["price"], 0)

        self.assertGreaterEqual(total_models, 50)
        self.assertGreaterEqual(total_variants, 100)

if __name__ == "__main__":
    unittest.main()
