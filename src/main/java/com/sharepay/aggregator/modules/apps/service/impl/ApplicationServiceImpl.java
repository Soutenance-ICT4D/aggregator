package com.sharepay.aggregator.modules.apps.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.apps.dto.request.CreateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.request.UpdateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApplicationResponse;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApiKeyRepository;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.apps.service.ApplicationService;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.shared.constant.AppStatus;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final String WEBHOOK_SECRET_PREFIX = "whsec_";
    private static final int WEBHOOK_SECRET_BYTES = 32; // 64 hex chars

    private final ApplicationRepository applicationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ApplicationResponse createApplication(UUID userId, CreateApplicationRequest request) {
        User user = userRepository.getReferenceById(userId);

        // Vérifier l'unicité du nom d'application pour cet utilisateur
        if (applicationRepository.existsByUserAndName(user, request.getName())) {
            throw new BusinessException(
                    "Vous avez déjà une application avec ce nom",
                    HttpStatus.CONFLICT,
                    "APP_NAME_ALREADY_EXISTS"
            );
        }

        // 3. Générer le webhook secret (valeur brute stockée en clair — nécessaire pour signer les webhooks côté serveur)
        String webhookSecret = generateWebhookSecret();

        // 4. Créer l'application
        Application application = Application.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .websiteUrl(request.getWebsiteUrl())
                .themeColor(request.getThemeColor())
                .currency(request.getCurrency() != null ? request.getCurrency() : "XAF")
                .webhookUrl(request.getWebhookUrl())
                .webhookSecret(webhookSecret)
                .successUrl(request.getSuccessUrl())
                .cancelUrl(request.getCancelUrl())
                .build();

        application = applicationRepository.save(application);
        log.info("Application '{}' créée pour l'utilisateur {}", application.getName(), userId);

        // Le secret est retourné une seule fois ici — il ne sera plus accessible après cet appel
        return toResponse(application, webhookSecret);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplications(UUID userId) {
        User user = userRepository.getReferenceById(userId);

        List<Application> apps = applicationRepository
                .findByUserAndStatusNotOrderByCreatedAtDesc(user, AppStatus.DELETED);

        // Charge toutes les clés actives en une seule requête SQL puis indexe par appId
        Map<UUID, ApiKey> activeKeyByAppId = apiKeyRepository
                .findByApplicationInAndIsActiveTrue(apps)
                .stream()
                .collect(Collectors.toMap(k -> k.getApplication().getId(), k -> k));

        return apps.stream()
                .map(app -> toResponse(app, null, activeKeyByAppId.get(app.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID userId, UUID appId) {
        User user = userRepository.getReferenceById(userId);

        Application application = applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));

        return toResponse(application);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplication(UUID userId, UUID appId, UpdateApplicationRequest request) {
        User user = userRepository.getReferenceById(userId);

        Application application = applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));

        // Vérifier l'unicité du nom uniquement si le nom change
        if (request.getName() != null && !request.getName().equals(application.getName())) {
            if (applicationRepository.existsByUserAndName(user, request.getName())) {
                throw new BusinessException(
                        "Vous avez déjà une application avec ce nom",
                        HttpStatus.CONFLICT,
                        "APP_NAME_ALREADY_EXISTS"
                );
            }
            application.setName(request.getName());
        }

        // Mise à jour des seuls champs fournis (PATCH)
        if (request.getDescription() != null) application.setDescription(request.getDescription());
        if (request.getLogoUrl() != null)      application.setLogoUrl(request.getLogoUrl());
        if (request.getWebsiteUrl() != null)   application.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getThemeColor() != null)   application.setThemeColor(request.getThemeColor());
        if (request.getCurrency() != null)     application.setCurrency(request.getCurrency());
        if (request.getWebhookUrl() != null)   application.setWebhookUrl(request.getWebhookUrl());
        if (request.getSuccessUrl() != null)   application.setSuccessUrl(request.getSuccessUrl());
        if (request.getCancelUrl() != null)    application.setCancelUrl(request.getCancelUrl());

        application = applicationRepository.save(application);
        log.info("Application '{}' mise à jour par l'utilisateur {}", application.getName(), userId);

        return toResponse(application);
    }

    @Override
    @Transactional
    public ApplicationResponse rotateWebhookSecret(UUID userId, UUID appId) {
        User user = userRepository.getReferenceById(userId);

        Application application = applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));

        String newWebhookSecret = generateWebhookSecret();
        application.setWebhookSecret(newWebhookSecret);
        applicationRepository.save(application);

        log.info("Webhook secret renouvelé pour l'application '{}' (user {})", application.getName(), userId);

        // Le nouveau secret est retourné une seule fois — il ne sera plus accessible après cet appel
        return toResponse(application, newWebhookSecret);
    }

    @Override
    @Transactional
    public void deleteApplication(UUID userId, UUID appId) {
        User user = userRepository.getReferenceById(userId);

        Application application = applicationRepository
                .findByIdAndUserAndStatusNot(appId, user, AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));

        application.setStatus(AppStatus.DELETED);
        applicationRepository.save(application);
        log.info("Application '{}' supprimée (soft delete) par l'utilisateur {}", application.getName(), userId);
    }

    private ApplicationResponse toResponse(Application app) {
        // Appel unitaire (get, update, rotate…) : on accepte la requête supplémentaire
        ApiKey activeKey = apiKeyRepository.findByApplicationAndIsActiveTrue(app).orElse(null);
        return toResponse(app, null, activeKey);
    }

    private ApplicationResponse toResponse(Application app, String plainTextWebhookSecret) {
        ApiKey activeKey = apiKeyRepository.findByApplicationAndIsActiveTrue(app).orElse(null);
        return toResponse(app, plainTextWebhookSecret, activeKey);
    }

    private ApplicationResponse toResponse(Application app, String plainTextWebhookSecret, ApiKey activeKey) {
        ApplicationResponse.ApplicationResponseBuilder builder = ApplicationResponse.builder()
                .id(app.getId())
                .name(app.getName())
                .description(app.getDescription())
                .logoUrl(app.getLogoUrl())
                .websiteUrl(app.getWebsiteUrl())
                .themeColor(app.getThemeColor())
                .currency(app.getCurrency())
                .webhookUrl(app.getWebhookUrl())
                .plainTextWebhookSecret(plainTextWebhookSecret)
                .successUrl(app.getSuccessUrl())
                .cancelUrl(app.getCancelUrl())
                .status(app.getStatus())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt());

        if (activeKey != null) {
            builder.activeKeyPrefix(activeKey.getKeyPrefix());
            builder.activeKeyEnvironment(activeKey.getEnvironment());
        }

        return builder.build();
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[WEBHOOK_SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return WEBHOOK_SECRET_PREFIX + HexFormat.of().formatHex(bytes);
    }
}
