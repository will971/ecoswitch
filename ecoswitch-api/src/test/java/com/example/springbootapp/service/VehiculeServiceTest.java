package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.springbootapp.model.entity.FuelType;
import com.example.springbootapp.model.entity.Vehicule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VehiculeServiceTest {

	@Autowired
	private VehiculeService vehiculeService;

	@Test
	void shouldValidateAndCreateVehicule() {
		Vehicule v = new Vehicule();
		v.setName("My Peugeot 208");
		v.setBrand("Peugeot");
		v.setModel("208");
		v.setVersion("1.2 PureTech");
		v.setFuelType(FuelType.PETROL);
		v.setUrl("https://www.peugeot.fr");

		Vehicule created = vehiculeService.create(v);
		assertNotNull(created.getId());
		assertEquals("My Peugeot 208", created.getName());

		Vehicule found = vehiculeService.findById(created.getId());
		assertEquals("Peugeot", found.getBrand());
	}

	@Test
	void shouldThrowExceptionWhenNameIsMissing() {
		Vehicule v = new Vehicule();
		v.setBrand("Peugeot");
		v.setModel("208");
		v.setVersion("1.2 PureTech");
		v.setFuelType(FuelType.PETROL);

		assertThrows(IllegalArgumentException.class, () -> vehiculeService.create(v));
	}

	@Test
	void shouldThrowExceptionWhenUrlIsInvalid() {
		Vehicule v = new Vehicule();
		v.setName("My Car");
		v.setBrand("Peugeot");
		v.setModel("208");
		v.setVersion("1.2 PureTech");
		v.setFuelType(FuelType.PETROL);
		v.setUrl("invalid_url");

		assertThrows(IllegalArgumentException.class, () -> vehiculeService.create(v));
	}

	@Test
	void shouldUpdateAndFindVehicule() {
		Vehicule v = new Vehicule();
		v.setName("Old Name");
		v.setBrand("Brand");
		v.setModel("Model");
		v.setVersion("Version");
		v.setFuelType(FuelType.PETROL);
		Vehicule created = vehiculeService.create(v);

		created.setName("New Name");
		Vehicule updated = vehiculeService.update(created.getId(), created);
		assertEquals("New Name", updated.getName());
	}

	@Test
	void shouldDeleteVehicule() {
		Vehicule v = new Vehicule();
		v.setName("To Delete");
		v.setBrand("Brand");
		v.setModel("Model");
		v.setVersion("Version");
		v.setFuelType(FuelType.PETROL);
		Vehicule created = vehiculeService.create(v);

		assertNotNull(vehiculeService.findById(created.getId()));
		vehiculeService.delete(created.getId());

		assertThrows(IllegalArgumentException.class, () -> vehiculeService.findById(created.getId()));
	}
}
