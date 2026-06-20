package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
class AdemeServiceTest {

	@Autowired
	private AdemeService ademeService;

	@Test
	void shouldLoadHistoricalAndAdemeData() {
		List<String> brands = ademeService.getBrands();
		assertNotNull(brands);
		assertFalse(brands.isEmpty());
		
		// Check normalized brand name exists
		assertTrue(brands.contains("BMW"));
		assertTrue(brands.contains("Renault"));

		List<String> models = ademeService.getModels("BMW");
		assertNotNull(models);
		assertFalse(models.isEmpty());
		
		// BMW models should have normalised rules applied (e.g. from model-mappings.json)
		assertTrue(models.stream().anyMatch(m -> m.contains("Série 1 (F40)")));

		// Get versions for a specific model
		List<AdemeService.AdemeVehicle> versions = ademeService.getVersions("BMW", "Série 1 (F40)");
		assertNotNull(versions);

		// Get single vehicle
		if (!versions.isEmpty()) {
			AdemeService.AdemeVehicle first = versions.get(0);
			var found = ademeService.getVehicle("BMW", "Série 1 (F40)", first.version());
			assertTrue(found.isPresent());
			assertEquals(first.brand(), found.get().brand());
			assertEquals(first.model(), found.get().model());
			assertEquals(first.version(), found.get().version());
		}
	}
}
