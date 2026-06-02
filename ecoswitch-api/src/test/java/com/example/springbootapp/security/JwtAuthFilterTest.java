package com.example.springbootapp.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.springbootapp.service.JwtService;

import io.jsonwebtoken.JwtException;

class JwtAuthFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotCrashWhenExtractEmailThrowsEvenIfTokenValid() {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractEmail("t")).thenThrow(new JwtException("bad claims"));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer t");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

