package com.example.springbootapp.controller.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.springbootapp.service.JwtService;
import com.example.springbootapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuthControllerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtService jwtService = mock(JwtService.class);
	private final UserService userService = mock(UserService.class);
	private final AuthController authController = new AuthController(objectMapper, jwtService, userService);

	@Test
	void shouldReturnBadRequestWhenCredentialIsMissing() {
		ResponseEntity<?> response = authController.googleLogin(Map.of());
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("Le jeton Google (credential) est manquant.", body.get("error"));
	}

	@Test
	void shouldReturnBadRequestWhenCredentialIsEmpty() {
		ResponseEntity<?> response = authController.googleLogin(Map.of("credential", "  "));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}

	@Test
	@Disabled("Évite un test réseau (Google tokeninfo) fragile en CI/offline.")
	void shouldReturnUnauthorizedWhenCredentialIsInvalidOrExpired() {
		// Ce test effectue un appel reseau fictif avec un token invalide.
		// Google retournera une erreur 400 Bad Request que notre backend intercepte et transforme en 401 Unauthorized.
		ResponseEntity<?> response = authController.googleLogin(Map.of("credential", "invalid_token_test"));
		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		
		Map<?, ?> body = (Map<?, ?>) response.getBody();
		assertNotNull(body);
		assertEquals("Jeton d'authentification Google invalide ou expiré.", body.get("error"));
	}
}
