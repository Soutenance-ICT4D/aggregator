package com.sharepay.aggregator.modules.webhook.controller;

import com.sharepay.aggregator.modules.webhook.dto.request.UpdateWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookConfigResponse;
import com.sharepay.aggregator.modules.webhook.service.WebhookService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "Configuration et test des webhooks d'application")
@SecurityRequirement(name = "apiKeyAuth")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @Operation(
            summary = "Consulter la configuration webhook",
            description = "Retourne l'URL et le préfixe masqué du secret webhook pour l'application liée à la clé API. " +
                    "Pour obtenir le secret complet, utilisez la rotation via `POST /api/v1/merchants/apps/{appId}/webhook-secret/rotate`."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration webhook retournée avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = WebhookConfigResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<WebhookConfigResponse> getConfig(
            @AuthenticationPrincipal String apiKeyId
    ) {
        WebhookConfigResponse response = webhookService.getConfig(UUID.fromString(apiKeyId));
        return ApiResponse.success(response);
    }

    @PatchMapping
    @Operation(
            summary = "Configurer l'URL webhook",
            description = "Met à jour l'URL qui recevra les notifications webhook de l'application. " +
                    "Les événements envoyés sont signés avec HMAC-SHA256 via l'en-tête `X-Sharepay-Signature`."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configuration webhook mise à jour avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = WebhookConfigResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "`VALIDATION_ERROR` - l'URL webhook fournie est manquante ou mal formée, ou `MALFORMED_JSON`",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<WebhookConfigResponse> updateConfig(
            @Valid @RequestBody UpdateWebhookRequest request,
            @AuthenticationPrincipal String apiKeyId
    ) {
        WebhookConfigResponse response = webhookService.updateConfig(UUID.fromString(apiKeyId), request);
        return ApiResponse.success("Configuration webhook mise à jour.", response);
    }

}
