package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    @Test
    void generateTokenShouldFailFastWhenSecretMissing() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", null);
        ReflectionTestUtils.setField(jwtService, "expirationDays", 7);

        assertThrows(IllegalStateException.class, () -> jwtService.generateToken("a@b.com"));
    }

    @Test
    void extractEmailShouldFailFastWhenSecretBlank() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "   ");
        ReflectionTestUtils.setField(jwtService, "expirationDays", 7);

        assertThrows(IllegalStateException.class, () -> jwtService.extractEmail("token"));
    }
}

