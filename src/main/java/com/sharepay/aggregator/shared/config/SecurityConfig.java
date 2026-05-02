package com.sharepay.aggregator.shared.config;

import com.sharepay.aggregator.shared.security.RateLimitingFilter;
import com.sharepay.aggregator.shared.security.apikey.ApiKeyAuthenticationFilter;
import com.sharepay.aggregator.shared.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/**")
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Assets statiques (logo, favicons, images providers) ──
                        .requestMatchers("/logo_sharepay_svg.svg", "/favicon.ico", "/assets/**", "/swagger-custom.js",
                                "/*.png", "/*.jpg", "/*.jpeg", "/*.svg", "/*.webp").permitAll()

                        .requestMatchers("/v3/api-docs/**").permitAll()
                        // Autoriser les ressources statiques (CSS, JS, images)
                        .requestMatchers("/swagger-ui/*.css", "/swagger-ui/*.js", "/swagger-ui/*.png", "/swagger-ui/*.jpg", "/swagger-ui/*.svg", "/swagger-ui/favicon-*.png", "/swagger-ui/oauth2-redirect.html").permitAll()
                        // Bloquer uniquement les pages HTML standards
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/").denyAll()

                        // ── Endpoints publics ──
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/checkout/**").permitAll()
                        .requestMatchers("/collect/**").permitAll()
                        .requestMatchers("/download-ticket/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Endpoints Merchant (JWT avec rôle MERCHANT requis) ──
                        .requestMatchers("/api/v1/merchants/**").hasRole("MERCHANT")
                        .requestMatchers("/api/v1/apps/**").hasRole("MERCHANT")
                        // ── Endpoints Admin (JWT avec rôle ADMIN requis) ──
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ── Endpoints Support (JWT avec rôle SUPPORT requis) ──
                        .requestMatchers("/api/v1/support/**").hasRole("SUPPORT")

                        // ── Endpoints API Key (intégrations B2B) ──
                        .requestMatchers("/api/v1/integration/**").hasRole("API_CLIENT")
                        .requestMatchers("/api/v1/pay-in/**").hasRole("API_CLIENT")
                        .requestMatchers("/api/v1/pay-out/**").hasRole("API_CLIENT")
                        .requestMatchers("/api/v1/webhook/**").hasRole("API_CLIENT")
                        .requestMatchers("/api/v1/webhook").hasRole("API_CLIENT")

                        // ── Tous les autres endpoints nécessitent une authentification ──
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"UNAUTHORIZED\",\"data\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"message\":\"ACCESS_DENIED\",\"data\":null,\"timestamp\":\"" + LocalDateTime.now() + "\"}"
                            );
                        })
                )
                // Ajout des filtres dans l'ordre : rate limiting -> api key -> jwt
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configuration CORS globale.
     * Permet les requêtes cross-origin depuis n'importe quelle origine.
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("*"));
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}