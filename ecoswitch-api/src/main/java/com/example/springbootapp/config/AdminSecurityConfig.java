package com.example.springbootapp.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import com.example.springbootapp.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class AdminSecurityConfig {

    @Value("${app.security.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if ("*".equals(allowedOrigins.trim())) {
            configuration.addAllowedOriginPattern("*");
            configuration.setAllowCredentials(true);
        } else {
            configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
            configuration.setAllowCredentials(true);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ── Chain 1 : API REST — Stateless + JWT ────────────────────────────────
    // S'applique en priorité aux routes /api/** et /h2-console/**

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .securityMatcher("/api/**", "/h2-console/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
