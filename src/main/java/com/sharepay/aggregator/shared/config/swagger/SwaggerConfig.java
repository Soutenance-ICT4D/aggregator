package com.sharepay.aggregator.shared.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
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
                        "/api/v1/auth/**",
                        "/api/v1/account/**",
                        "/api/v1/apps/**",
                        "/api/v1/fund-collection/**",
                        "/api/v1/payment/**",
                        "/api/v1/payout/**",
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
                            new Tag().name("Authentification Marchand").description("Endpoints d'authentification des marchands"),
                            new Tag().name("Compte Marchand").description("Endpoints de gestion du compte marchand"),
                            new Tag().name("Applications").description("Endpoints de gestion des applications marchandes"),
                            new Tag().name("API Keys").description("Endpoints de gestion des clés API des applications marchandes"),
                            new Tag().name("Collecte de fonds").description("Endpoints de gestion des collectes de fonds"),
                            new Tag().name("Administration").description("Endpoints de gestion des utilisateurs et du système"),
                            new Tag().name("Support").description("Endpoints d'outils de support client")
                    ));

                    // Schéma de sécurité : JWT Bearer
                    openApi.getComponents().addSecuritySchemes("bearerAuth",
                            new SecurityScheme()
                                    .name("bearerAuth")
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT Bearer Token obtenu après login")
                    );

                    // Appliquer la sécurité par défaut sur tous les endpoints
                    openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
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
                        "/api/v1/payments/**",
                        "/api/v1/payouts/**"
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
                            new Tag().name("Paiements").description("Initiation et suivi des paiements"),
                            new Tag().name("Retraits").description("Retraits et remboursements"),
                            new Tag().name("Webhooks").description("Gestion des webhooks et événements")
                    ));

                    // Schéma de sécurité : API Key
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

                    // Appliquer la sécurité par défaut sur tous les endpoints
                    openApi.addSecurityItem(new SecurityRequirement().addList("apiKeyAuth"));
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