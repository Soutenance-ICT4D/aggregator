package com.sharepay.aggregator.shared.security.apikey;

import com.sharepay.aggregator.modules.apps.dto.response.ApiKeyResponse;
import com.sharepay.aggregator.modules.apps.service.ApiKeyService;
import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import com.sharepay.aggregator.shared.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre d'authentification par API Key pour les intégrations B2B.
 *
 * Extrait et valide l'API Key depuis le header X-API-KEY.
 * Si la clé est valide et active, configure le contexte de sécurité Spring Security.
 *
 * Format attendu : X-API-KEY: sk_live_... ou sk_test_...
 *
 * Endpoints concernés :
 * - /api/v1/integration/**
 * - /api/v1/payments/**
 * - /api/v1/payouts/**
 *
 * Note : Les endpoints /api/v1/public/** sont accessibles sans API Key.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isPublicPath(path) || isJwtPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawApiKey = request.getHeader(API_KEY_HEADER);

        if (rawApiKey == null || rawApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiKeyResponse keyDetails = apiKeyService.validateApiKey(rawApiKey);
            boolean isLiveMode = keyDetails.getEnvironment() == ApiKeyEnvironment.LIVE;

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                    new SimpleGrantedAuthority(isLiveMode ? "ROLE_LIVE_MODE" : "ROLE_TEST_MODE")
            );

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    keyDetails.getId().toString(),
                    rawApiKey,
                    authorities
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("API Key authentifiée : {} (mode: {})", keyDetails.getKeyPrefix(), isLiveMode ? "live" : "test");

        } catch (BusinessException e) {
            log.warn("Tentative d'accès avec une clé API invalide : {}", e.getMessage());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la validation de l'API Key : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/public/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/actuator/health") ||
                path.equals("/logo_sharepay_svg.svg");
    }

    private boolean isJwtPath(String path) {
        return path.startsWith("/api/v1/apps/") ||
                path.startsWith("/api/v1/merchants/") ||
                path.startsWith("/api/v1/webhook/") ||
                path.startsWith("/api/v1/admin/") ||
                path.startsWith("/api/v1/support/");
    }
}