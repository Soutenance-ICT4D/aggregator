package com.sharepay.aggregator.modules.apps.controller;

import com.sharepay.aggregator.modules.apps.dto.request.UpdateAppWebhookRequest;
import com.sharepay.aggregator.modules.apps.dto.response.AppWebhookResponse;
import com.sharepay.aggregator.modules.apps.service.AppWebhookService;
import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import com.sharepay.aggregator.modules.webhook.dto.request.TestWebhookRequest;
import com.sharepay.aggregator.modules.webhook.dto.response.TestWebhookResponse;
import com.sharepay.aggregator.modules.webhook.dto.response.WebhookDeliveryResponse;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps/{appId}/webhook")
@RequiredArgsConstructor
@Tag(name = "Webhook", description = "Gestion de la configuration webhook d'une application (dashboard merchant)")
@SecurityRequirement(name = "bearerAuth")
public class AppWebhookController {

    private final AppWebhookService appWebhookService;

    @GetMapping
    @Operation(
            summary = "Consulter la configuration webhook",
            description = "Retourne l'URL et le préfixe masqué du secret webhook actuel. " +
                    "`webhookSecretPrefix` est null si aucun secret n'a encore été créé."
    )
    public ApiResponse<AppWebhookResponse> getWebhookConfig(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(appWebhookService.getWebhookConfig(currentUser.getAccountId(), appId));
    }

    @PatchMapping
    @Operation(
            summary = "Mettre à jour l'URL webhook",
            description = "Modifie uniquement l'URL de réception des événements webhook. " +
                    "Le secret reste inchangé. Passer une valeur vide ou nulle pour désactiver les notifications."
    )
    public ApiResponse<AppWebhookResponse> updateWebhookUrl(
            @PathVariable UUID appId,
            @Valid @RequestBody UpdateAppWebhookRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        AppWebhookResponse response = appWebhookService.updateWebhookUrl(currentUser.getAccountId(), appId, request);
        return ApiResponse.success("URL webhook mise à jour.", response);
    }

    @PostMapping("/secret")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Créer un webhook secret",
            description = "Génère un webhook secret pour la première fois. " +
                    "La valeur brute (plainTextWebhookSecret) est retournée **une seule fois** — stockez-la immédiatement. " +
                    "Retourne une erreur si un secret existe déjà — utilisez `/rotate` dans ce cas."
    )
    public ApiResponse<AppWebhookResponse> createSecret(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        AppWebhookResponse response = appWebhookService.createSecret(currentUser.getAccountId(), appId);
        return ApiResponse.success("Webhook secret créé. Conservez la valeur affichée, elle ne sera plus accessible.", response);
    }

    @PostMapping("/rotate")
    @Operation(
            summary = "Rotation du webhook secret",
            description = "Remplace le secret actuel par un nouveau. L'ancien est immédiatement invalidé. " +
                    "La valeur brute (plainTextWebhookSecret) est retournée **une seule fois** — stockez-la immédiatement. " +
                    "Retourne une erreur si aucun secret n'existe — utilisez `POST /secret` d'abord."
    )
    public ApiResponse<AppWebhookResponse> rotateSecret(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        AppWebhookResponse response = appWebhookService.rotateSecret(currentUser.getAccountId(), appId);
        return ApiResponse.success("Webhook secret renouvelé. L'ancien secret est révoqué. Conservez la nouvelle valeur affichée.", response);
    }

    @DeleteMapping("/secret")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Révoquer le webhook secret",
            description = "Supprime le secret webhook. Les webhooks seront toujours envoyés si une URL est configurée, " +
                    "mais sans l'en-tête `X-Sharepay-Signature`. Cette action est irréversible — créez un nouveau secret via `POST /secret`."
    )
    public void revokeSecret(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        appWebhookService.revokeSecret(currentUser.getAccountId(), appId);
    }

    @PostMapping("/test")
    @Operation(
            summary = "Tester le webhook",
            description = "Envoie un payload `webhook.test` vers l'URL configurée. " +
                    "Signé avec HMAC-SHA256 si un secret est configuré, envoyé sans signature sinon. " +
                    "Le champ `data` est libre pour personnaliser le contenu."
    )
    public ApiResponse<TestWebhookResponse> testWebhook(
            @PathVariable UUID appId,
            @RequestBody(required = false) TestWebhookRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        TestWebhookResponse response = appWebhookService.testWebhook(currentUser.getAccountId(), appId, request);
        return ApiResponse.success("Webhook de test envoyé.", response);
    }

    @GetMapping("/deliveries")
    @Operation(
            summary = "Historique des livraisons webhook",
            description = "Retourne la liste paginée des tentatives de livraison webhook pour cette application. " +
                    "Filtrable par statut (`PENDING`, `DELIVERED`, `FAILED`) via le paramètre `status`."
    )
    public ApiResponse<Page<WebhookDeliveryResponse>> listDeliveries(
            @PathVariable UUID appId,
            @RequestParam(required = false) WebhookDeliveryStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        Page<WebhookDeliveryResponse> page = appWebhookService.listDeliveries(
                currentUser.getAccountId(), appId, status, pageable);
        return ApiResponse.success(page);
    }

    @PostMapping("/deliveries/{deliveryId}/replay")
    @Operation(
            summary = "Rejouer une livraison webhook",
            description = "Crée une nouvelle tentative de livraison pour l'événement indiqué, " +
                    "en utilisant l'URL et le secret webhook actuels. " +
                    "La livraison originale reste inchangée dans l'historique."
    )
    public ApiResponse<WebhookDeliveryResponse> replayDelivery(
            @PathVariable UUID appId,
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        WebhookDeliveryResponse response = appWebhookService.replayDelivery(
                currentUser.getAccountId(), appId, deliveryId);
        return ApiResponse.success("Livraison rejouée.", response);
    }
}
