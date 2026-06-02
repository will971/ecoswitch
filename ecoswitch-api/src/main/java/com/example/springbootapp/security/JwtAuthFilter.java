package com.example.springbootapp.security;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.springbootapp.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre Spring Security qui intercepte chaque requête, lit le header
 * {@code Authorization: Bearer <token>}, valide le JWT et renseigne
 * le {@link SecurityContextHolder} avec l'email de l'utilisateur.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isTokenValid(token)) {
                try {
                    String email = jwtService.extractEmail(token);

                    // Pas d'autorités (roles) pour l'instant — extension facile
                    var authentication = new UsernamePasswordAuthenticationToken(
                            email,                          // principal = email
                            null,                           // credentials
                            Collections.emptyList()         // authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException e) {
                    // Défense en profondeur: même si la validation a "réussi", une extraction peut encore échouer.
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
