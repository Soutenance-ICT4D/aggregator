package com.sharepay.aggregator.shared.security.apikey;

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
 * Si la clé est valide, configure le contexte de sécurité Spring Security.
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

    // TODO: Injecter ApiKeyService pour la validation en base de données
    // private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Chemins publics qui ne nécessitent pas d'API Key
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Chemins qui utilisent JWT au lieu d'API Key
        if (isJwtPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraction de l'API Key depuis le header
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.trim().isEmpty()) {
            // Pas d'API Key → laisser passer (Spring Security refusera l'accès si nécessaire)
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // TODO: Valider l'API Key via ApiKeyService
            // ApiKeyDetails keyDetails = apiKeyService.validateAndGetDetails(apiKey);
            // String merchantId = keyDetails.getMerchantId();
            // boolean isLiveMode = keyDetails.isLiveMode();

            // ⚠️ TEMPORAIRE : Simulation de la validation
            // À REMPLACER par la vraie validation en base de données
            if (isValidApiKey(apiKey)) {
                // Simuler les données extraites de la base
                String merchantId = extractMerchantId(apiKey);
                boolean isLiveMode = apiKey.startsWith("sk_live_");

                // Créer les authorities (rôles)
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_API_CLIENT"),
                        new SimpleGrantedAuthority(isLiveMode ? "ROLE_LIVE_MODE" : "ROLE_TEST_MODE")
                );

                // Créer le token d'authentification Spring Security
                // Le principal contient l'ID du marchand
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        merchantId,  // Principal (ID du marchand)
                        apiKey,      // Credentials (API Key pour audit)
                        authorities  // Authorities (rôles)
                );

                // Ajouter les détails de la requête
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Configurer le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("API Key authentification réussie pour le marchand : {} (mode: {})",
                        merchantId, isLiveMode ? "live" : "test");
            }

        } catch (Exception e) {
            log.error("Erreur lors de la validation de l'API Key : {}", e.getMessage());
            // En cas d'erreur, ne pas bloquer la requête, Spring Security refusera l'accès
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Vérifie si le chemin est public et ne nécessite pas d'API Key.
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/public/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/actuator/health") ||
                path.equals("/logo_sharepay_svg.svg");
    }

    /**
     * Vérifie si le chemin utilise l'authentification JWT plutôt qu'API Key.
     */
    private boolean isJwtPath(String path) {
        return path.startsWith("/api/v1/apps/") ||
                path.startsWith("/api/v1/payment-links/") ||
                path.startsWith("/api/v1/merchants/") ||
                path.startsWith("/api/v1/admin/") ||
                path.startsWith("/api/v1/support/");
    }

    /**
     * Validation temporaire de l'API Key.
     *
     * ⚠️ À REMPLACER par une vraie validation via ApiKeyService qui :
     * - Vérifie que la clé existe en base de données
     * - Vérifie que la clé est active (non révoquée)
     * - Récupère les infos du marchand associé
     * - Vérifie les permissions et limites de la clé
     */
    private boolean isValidApiKey(String apiKey) {
        // TODO: Implémenter la vraie validation
        // return apiKeyService.isValidAndActive(apiKey);

        // Validation basique du format
        if (apiKey == null || apiKey.length() < 35) {
            return false;
        }

        // Vérifier le préfixe
        return apiKey.startsWith("sk_live_") || apiKey.startsWith("sk_test_");
    }

    /**
     * Extrait l'ID du marchand depuis l'API Key.
     *
     * ⚠️ TEMPORAIRE : Simulation
     * En production, cette info vient de la base de données.
     */
    private String extractMerchantId(String apiKey) {
        // TODO: Récupérer depuis la base de données
        // ApiKey key = apiKeyService.findByKey(apiKey);
        // return key.getMerchantId();

        // Temporairement, générer un ID fictif
        return "merchant_" + Math.abs(apiKey.hashCode());
    }
}