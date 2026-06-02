package com.example.springbootapp.controller.vehicule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

class ImmatriculationControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ResourceLoader resourceLoader = new DefaultResourceLoader();
	private final ImmatriculationController immatriculationController = new ImmatriculationController(objectMapper, resourceLoader);

	@Test
	void shouldReturnBadRequestWhenPlaqueIsMissing() {
		ResponseEntity<?> response = immatriculationController.rechercherPlaque("   ");
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	void shouldFindBmwFromLocalDatabaseWithCleanPlaque() {
		// Plaque avec espaces et tirets, minuscules
		ResponseEntity<?> response = immatriculationController.rechercherPlaque("ez-999-zz");
		assertEquals(HttpStatus.OK, response.getStatusCode());
		
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("BMW 114i 1.6 102cv (2013)", body.get("name"));
		assertEquals("PETROL", body.get("fuelType"));
		assertEquals(6.5, body.get("consumption"));
		assertEquals("LOCAL_FALLBACK", body.get("source"));
	}

	@Test
	void shouldReturnNotFoundWhenPlaqueIsUnknown() {
		// Une plaque inconnue doit renvoyer 404 strict au lieu de l'Estimative Engine
		ResponseEntity<?> response = immatriculationController.rechercherPlaque("CD-456-EF");
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("Plaque d'immatriculation introuvable. Veuillez saisir manuellement les informations.", body.get("error"));
	}

	@Test
	void shouldReturnNotFoundWhenPlaqueFormatIsTotallyInvalid() {
		ResponseEntity<?> response = immatriculationController.rechercherPlaque("plaquebidon");
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}
}
