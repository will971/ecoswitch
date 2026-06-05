package com.example.springbootapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.springbootapp.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class AdminSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // ── Chain 1 : API REST — Stateless + JWT ────────────────────────────────
    // S'applique en priorité aux routes /api/** et /h2-console/**

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .securityMatcher("/api/**", "/h2-console/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin())) // H2 console
            .authorizeHttpRequests(auth -> auth
                // Auth et routes publiques
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/vehicules/**",
                                 "/api/v1/comparisons/**",
                                 "/api/v1/immatriculation/**",
                                 "/api/v1/ademe/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Simulations — JWT obligatoire
                .requestMatchers("/api/v1/simulations/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // 401 JSON pour les API protégées sans token
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                (request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Token JWT manquant ou invalide.\"}");
                }
            ));

        return http.build();
    }

    // ── Chain 2 : Interface Admin — Session + Form Login ────────────────────
    // S'applique à tout le reste (admin/**, swagger-ui/**, etc.)

    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/admin/login", "/admin/login.html").permitAll()
                .requestMatchers("/admin/**", "/api/v1/admin/**").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin/", true)
                .permitAll()
            );

        return http.build();
    }

    // ── UserDetailsService pour l'admin (in-memory) ──────────────────────────

    @Bean
    public UserDetailsService userDetailsService(AppAdminProperties adminProperties,
                                                  PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username(adminProperties.getUsername())
                .password(passwordEncoder.encode(adminProperties.getPassword()))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
