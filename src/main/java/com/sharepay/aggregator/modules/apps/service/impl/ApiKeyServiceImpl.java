package com.sharepay.aggregator.modules.apps.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.apps.dto.request.CreateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.request.RotateApiKeyRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApiKeyResponse;
import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.apps.service.ApiKeyService;
import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final int KEY_RANDOM_BYTES = 64; // 128 hex chars
    private static final int KEY_PREFIX_DISPLAY_LENGTH = 8; // chars après le préfixe d'env
    private static final String LIVE_KEY_PREFIX = "sk_live_";
    private static final String TEST_KEY_PREFIX = "sk_test_";

    private final ApiKeyRepository apiKeyRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApiKeyResponse createApiKey(UUID userId, UUID appId, CreateApiKeyRequest request) {
        Application application = resolveApplication(userId, appId);

        // Une seule clé active par application (tous environnements confondus)
        if (apiKeyRepository.findByApplicationAndIsActiveTrue(application).isPresent()) {
            throw new BusinessException(
                    "Cette application possède déjà une clé API active. Révoquez-la ou utilisez la rotation avant d'en créer une nouvelle.",
                    HttpStatus.CONFLICT,
                    "API_KEY_ALREADY_ACTIVE"
            );
        }

        String rawKey = generateRawKey(request.getEnvironment());
        ApiKey apiKey = buildApiKey(application, request.getName(), rawKey, request.getEnvironment());
        apiKey = apiKeyRepository.save(apiKey);

        log.info("Clé API {} créée pour l'application '{}' (user {})", request.getEnvironment(), application.getName(), userId);
        return toResponse(apiKey, rawKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(UUID userId, UUID appId) {
        Application application = resolveApplication(userId, appId);

        return apiKeyRepository
                .findByApplicationOrderByCreatedAtDesc(application)
                .stream()
                .map(key -> toResponse(key, null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKeyResponse getApiKey(UUID userId, UUID appId) {
        Application application = resolveApplication(userId, appId);

        ApiKey apiKey = apiKeyRepository
                .findByApplicationAndIsActiveTrue(application)
                .orElseThrow(() -> new BusinessException(
                        "Aucune clé API active pour cette application.",
                        HttpStatus.NOT_FOUND,
                        "API_KEY_NOT_FOUND"
                ));

        return toResponse(apiKey, null);
    }

    @Override
    @Transactional
    public ApiKeyResponse rotateApiKey(UUID userId, UUID appId, RotateApiKeyRequest request) {
        Application application = resolveApplication(userId, appId);

        // Révoquer la clé active si elle existe
        apiKeyRepository.findByApplicationAndIsActiveTrue(application).ifPresent(k -> {
            k.setActive(false);
            apiKeyRepository.save(k);
        });

        // Créer la nouvelle clé
        String rawKey = generateRawKey(request.getEnvironment());
        ApiKey newKey = buildApiKey(application, request.getName(), rawKey, request.getEnvironment());
        newKey = apiKeyRepository.save(newKey);

        log.info("Rotation de clé API pour l'application '{}' : nouvelle clé {} créée (user {})",
                application.getName(), request.getEnvironment(), userId);

        return toResponse(newKey, rawKey);
    }

    @Override
    @Transactional
    public void revokeApiKey(UUID userId, UUID appId) {
        Application application = resolveApplication(userId, appId);

        ApiKey apiKey = apiKeyRepository
                .findByApplicationAndIsActiveTrue(application)
                .orElseThrow(() -> new BusinessException(
                        "Aucune clé API active à révoquer pour cette application.",
                        HttpStatus.NOT_FOUND,
                        "API_KEY_NOT_FOUND"
                ));

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        log.info("Clé API révoquée pour l'application '{}' (user {})", application.getName(), userId);
    }

    @Override
    @Transactional
    public ApiKeyResponse validateApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BusinessException("Clé API manquante.", HttpStatus.UNAUTHORIZED, "API_KEY_MISSING");
        }

        String keyHash = HashUtil.sha256(rawApiKey);

        ApiKey apiKey = apiKeyRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> new BusinessException("Clé API invalide.", HttpStatus.UNAUTHORIZED, "API_KEY_INVALID"));

        if (!apiKey.isActive()) {
            throw new BusinessException("Clé API révoquée.", HttpStatus.UNAUTHORIZED, "API_KEY_REVOKED");
        }

        apiKey.setLastUsedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        return toResponse(apiKey, null);
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    /**
     * Vérifie que l'application appartient bien à l'utilisateur et qu'elle n'est pas supprimée.
     */
    private Application resolveApplication(UUID userId, UUID appId) {
        User user = userRepository.getReferenceById(userId);

        return applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException("Application non trouvée.", HttpStatus.NOT_FOUND, "APP_NOT_FOUND"));
    }

    /**
     * Génère une clé brute au format {@code sk_live_<32 hex>} ou {@code sk_test_<32 hex>}.
     */
    private String generateRawKey(ApiKeyEnvironment environment) {
        byte[] bytes = new byte[KEY_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        String randomPart = HexFormat.of().formatHex(bytes);
        String envPrefix = environment == ApiKeyEnvironment.LIVE ? LIVE_KEY_PREFIX : TEST_KEY_PREFIX;
        return envPrefix + randomPart;
    }

    /**
     * Construit l'entité ApiKey à partir d'une clé brute.
     * Le keyPrefix (ex: {@code sk_live_a1b2c3d4}) est stocké pour l'affichage.
     * Le keyHash (SHA-256) est stocké pour la validation.
     */
    private ApiKey buildApiKey(Application application, String name, String rawKey, ApiKeyEnvironment environment) {
        String envPrefix = environment == ApiKeyEnvironment.LIVE ? LIVE_KEY_PREFIX : TEST_KEY_PREFIX;
        // Préfixe = "sk_live_" + les 8 premiers chars de la partie aléatoire
        String keyPrefix = rawKey.substring(0, envPrefix.length() + KEY_PREFIX_DISPLAY_LENGTH);

        return ApiKey.builder()
                .application(application)
                .name(name)
                .keyPrefix(keyPrefix)
                .keyHash(HashUtil.sha256(rawKey))
                .environment(environment)
                .build();
    }

    private ApiKeyResponse toResponse(ApiKey apiKey, String plainTextKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .environment(apiKey.getEnvironment())
                .active(apiKey.isActive())
                .lastUsedAt(apiKey.getLastUsedAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .plainTextKey(plainTextKey)
                .build();
    }
}
