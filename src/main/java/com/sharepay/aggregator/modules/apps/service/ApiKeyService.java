package com.sharepay.aggregator.modules.apps.service;

import com.sharepay.aggregator.modules.apps.dto.request.CreateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.request.RotateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApiKeyResponse;
import java.util.List;
import java.util.UUID;

public interface ApiKeyService {
    // Crée une clé API pour une application.
    ApiKeyResponse createApiKey(UUID userId, UUID appId, CreateApiKeyRequest request);

    //Retourne l'historique de toutes les clés API de l'application (actives + révoquées), triées par date de création décroissante.
    List<ApiKeyResponse> getApiKeys(UUID userId, UUID appId);

    // Retourne les informations de la clé API active de l'application.
    ApiKeyResponse getApiKey(UUID userId, UUID appId);

    // Révoque toutes les clés actives de l'application et génère une nouvelle clé.
    ApiKeyResponse rotateApiKey(UUID userId, UUID appId, RotateApiKeyRequest request);

    // Révoque la clé API active de l'application.
    void revokeApiKey(UUID userId, UUID appId);

    // Valide une clé API brute et retourne ses informations si elle est valide et active.
    ApiKeyResponse validateApiKey(String rawApiKey);
}
