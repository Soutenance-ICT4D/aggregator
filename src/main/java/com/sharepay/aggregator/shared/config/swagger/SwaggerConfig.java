package com.sharepay.aggregator.shared.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Configuration Swagger/OpenAPI pour la documentation des APIs Sharepay.
@Configuration
public class SwaggerConfig {

    // Configuration du groupe d'API "sharepay-frontend" pour les développeurs internes.
    @Bean
    public org.springdoc.core.models.GroupedOpenApi frontendApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("sharepay-frontend")
                .pathsToMatch(
                        "/api/v1/public/health",
                        "/api/v1/auth/**",
                        "/api/v1/account/**",
                        "/api/v1/merchants/**",
                        "/api/v1/merchants/apps/**",
                        "/api/v1/merchants/fund-collections/**",
                        "/api/v1/admin/**",
                        "/api/v1/support/**"
                )
                .addOpenApiCustomizer(openApi -> {
                    if (openApi.getComponents() == null) {
                        openApi.setComponents(new Components());
                    }

                    // Informations sur l'API
                    openApi.info(new Info()
                            .title("Sharepay Frontend API")
                            .description("API pour le portail Frontend Sharepay.")
                            .version("1.0.0")
                    );

                    // Ordre logique des tags dans l'UI (de haut en bas)
                    openApi.tags(List.of(
                            new Tag().name("Health Check").description("Endpoints pour vérifier l'état du système"),
                            new Tag().name("Authentification").description("Endpoints d'authentification des marchands"),
                            new Tag().name("Compte Marchand").description("Endpoints de gestion du compte marchand"),
                            new Tag().name("Transactions Marchand").description("Consultation des transactions du marchand connecté"),
                            new Tag().name("Applications Marchand").description("Endpoints de gestion des applications marchandes"),
                            new Tag().name("Clés API Marchand").description("Endpoints de gestion des clés API des applications marchandes"),
                            new Tag().name("Webhook Marchand").description("Gestion de la configuration webhook d'une application"),
                            new Tag().name("Collecte de fonds Marchand").description("Endpoints de gestion des collectes de fonds"),
                            new Tag().name("Retraits Marchand").description("Gestion des comptes et de la configuration de retrait"),
                            new Tag().name("Support").description("Endpoints d'outils de support client"),
                            new Tag().name("Marchands").description("Gestion des comptes marchands"),
                            new Tag().name("Providers").description("Gestion des opérateurs de paiement"),
                            new Tag().name("Support").description("Gestion des comptes admin et support"),
                            new Tag().name("Admin Transactions").description("Vue globale des transactions pay-in")
                    ));

                    // Schéma de sécurité : JWT Bearer
                    // Déclaré ici pour activer le bouton "Authorize" dans Swagger UI.
                    // Chaque controller sécurisé porte @SecurityRequirement(name = "bearerAuth")
                    // individuellement — les endpoints publics (auth, register…) restent sans cadenas.
                    openApi.getComponents().addSecuritySchemes("bearerAuth",
                            new SecurityScheme()
                                    .name("bearerAuth")
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT Bearer Token obtenu après login")
                    );
                })
                .build();
    }

    // Configuration du groupe d'API "sharepay-aggregator" pour les développeurs externes.
    @Bean
    public org.springdoc.core.models.GroupedOpenApi aggregatorApi() {
        return org.springdoc.core.models.GroupedOpenApi.builder()
                .group("sharepay-aggregator")
                .pathsToMatch(
                        "/api/v1/public/health",
                        "/api/v1/pay-in/**",
                        "/api/v1/pay-out/**",
                        "/api/v1/webhook",
                        "/api/v1/webhook/**"
                )
                .addOpenApiCustomizer(openApi -> {
                    if (openApi.getComponents() == null) {
                        openApi.setComponents(new Components());
                    }

                    // Informations sur l'API
                    openApi.info(new Info()
                            .title("Sharepay Integration API")
                            .description("API d'intégration B2B et Webhooks pour les partenaires Sharepay.")
                            .version("1.0.0")
                    );

                    // Ordre logique des tags dans l'UI
                    openApi.tags(List.of(
                            new Tag().name("Health Check").description("Endpoints pour vérifier l'état du système"),
                            new Tag().name("Paiement").description("Initiation et suivi des paiements entrants"),
                            new Tag().name("Retrait").description("Virements vers bénéficiaires"),
                            new Tag().name("Webhook").description("Configuration et test des webhooks d'application")
                    ));

                    // Schéma de sécurité : API Key
                    // Déclaré ici pour activer le bouton "Authorize" dans Swagger UI.
                    // Chaque controller sécurisé porte @SecurityRequirement(name = "apiKeyAuth")
                    // individuellement — les endpoints publics (/health…) restent sans cadenas.
                    openApi.getComponents().addSecuritySchemes("apiKeyAuth",
                            new SecurityScheme()
                                    .name("X-API-KEY")
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.HEADER)
                                    .description("""
                                        Votre clé API Sharepay.
                                        Format : `sk_live_...` (production) ou `sk_test_...` (test)
                                        """)
                    );
                })
                .build();
    }
    
    // Configuration OpenAPI globale (fallback si aucun groupe n'est sélectionné).
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sharepay Aggregator API")
                        .description("Documentation complète des APIs Sharepay")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sharepay")
                                .email("contact@sharepay.com")
                                .url("https://sharepay.com"))
                );
    }

    // Filtre de redirection Swagger
    @Bean
    public FilterRegistrationBean<Filter> swaggerRedirectFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        
        registrationBean.setFilter((request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String acceptHeader = httpRequest.getHeader("Accept");
            boolean isBrowserRequest = acceptHeader != null && acceptHeader.contains("text/html");

            if (isBrowserRequest) {
                String path = httpRequest.getRequestURI();
                String groupName = path.substring("/v3/api-docs/".length());
                
                if ("sharepay-aggregator".equals(groupName)) {
                    httpResponse.sendRedirect("/swagger-ui/devs/sharepay-docs");
                } else {
                    httpResponse.sendRedirect("/swagger-ui/devs/sharepay-integration");
                }
                return;
            }
            chain.doFilter(request, response);
        });
        
        // S'exécute en dernier pour ne pas bloquer les autres filtres
        registrationBean.setOrder(org.springframework.core.Ordered.LOWEST_PRECEDENCE);
        // On n'écoute que les accès aux descripteurs JSON
        registrationBean.addUrlPatterns("/v3/api-docs/*");
        
        return registrationBean;
    }
}