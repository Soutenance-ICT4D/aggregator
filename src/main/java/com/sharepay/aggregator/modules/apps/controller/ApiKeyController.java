package com.sharepay.aggregator.modules.apps.controller;

import com.sharepay.aggregator.modules.apps.dto.request.CreateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.request.RotateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApiKeyResponse;
import com.sharepay.aggregator.modules.apps.service.ApiKeyService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants/apps/{appId}/api-keys")
@RequiredArgsConstructor
@Tag(name = "Clés API Marchand", description = "Endpoints de gestion des clés API des applications marchandes")
@SecurityRequirement(name = "bearerAuth")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    // 1. Créer une clé API
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Créer une clé API",
            description = "Crée une clé API pour l'environnement spécifié. " +
                    "Une seule clé active par application est autorisée. " +
                    "La valeur brute de la clé (plainTextKey) est retournée **une seule fois** — stockez-la immédiatement."
    )
    public ApiResponse<ApiKeyResponse> createApiKey(
            @PathVariable UUID appId,
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApiKeyResponse response = apiKeyService.createApiKey(currentUser.getAccountId(), appId, request);
        return ApiResponse.success("Clé API créée avec succès. Conservez la valeur affichée, elle ne sera plus accessible.", response);
    }

    // 2. Historique de toutes les clés API (actives + révoquées)
    @GetMapping
    @Operation(
            summary = "Historique des clés API",
            description = "Retourne toutes les clés API de l'application, actives et révoquées, triées de la plus récente à la plus ancienne. " +
                    "La valeur brute des clés n'est jamais retournée ici."
    )
    public ApiResponse<List<ApiKeyResponse>> getApiKeys(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        List<ApiKeyResponse> keys = apiKeyService.getApiKeys(currentUser.getAccountId(), appId);
        return ApiResponse.success(keys);
    }

    // 3. Obtenir la clé API active
    @GetMapping("/active")
    @Operation(
            summary = "Obtenir la clé API active",
            description = "Retourne les informations de la clé API active de l'application. La valeur brute de la clé n'est jamais retournée ici."
    )
    public ApiResponse<ApiKeyResponse> getApiKey(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApiKeyResponse response = apiKeyService.getApiKey(currentUser.getAccountId(), appId);
        return ApiResponse.success(response);
    }

    // 4. Rotation de la clé API
    @PostMapping("/rotate")
    @Operation(
            summary = "Rotation de la clé API",
            description = "Révoque immédiatement la clé active de l'application " +
                    "et génère une nouvelle clé pour l'environnement spécifié. " +
                    "La valeur brute de la nouvelle clé (plainTextKey) est retournée **une seule fois** — stockez-la immédiatement. " +
                    "À utiliser en cas de compromission de clé."
    )
    public ApiResponse<ApiKeyResponse> rotateApiKey(
            @PathVariable UUID appId,
            @Valid @RequestBody RotateApiKeyRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApiKeyResponse response = apiKeyService.rotateApiKey(currentUser.getAccountId(), appId, request);
        return ApiResponse.success("Rotation effectuée. La clé précédente est révoquée. Conservez la nouvelle valeur affichée.", response);
    }

    // 5. Révoquer la clé API active
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Révoquer la clé API active",
            description = "Révoque la clé API active de l'application. " +
                    "Les appels utilisant cette clé seront immédiatement rejetés. Cette action est irréversible."
    )
    public void revokeApiKey(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        apiKeyService.revokeApiKey(currentUser.getAccountId(), appId);
    }
}
