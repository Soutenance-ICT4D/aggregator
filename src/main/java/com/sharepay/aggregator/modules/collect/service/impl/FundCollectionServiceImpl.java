package com.sharepay.aggregator.modules.collect.service.impl;

import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.apps.repository.ApplicationRepository;
import com.sharepay.aggregator.modules.collect.dto.request.CreateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.request.UpdateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.response.FundCollectionResponse;
import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.modules.collect.repository.FundCollectionRepository;
import com.sharepay.aggregator.modules.collect.service.FundCollectionService;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class FundCollectionServiceImpl implements FundCollectionService {

    private final FundCollectionRepository fundCollectionRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.collect-base-url:http://localhost:8080/collect/}")
    private String collectBaseUrl;

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public FundCollectionResponse createCollection(UUID userId, UUID applicationId, CreateFundCollectionRequest request) {
        Application application = resolveApplication(userId, applicationId);

        validateAmountConsistency(request.getIsAmountFixed(), request.getAmount());

        String slug = generateUniqueSlug(request.getTitle());

        FundCollection collection = FundCollection.builder()
                .application(application)
                .slug(slug)
                .title(request.getTitle())
                .description(request.getDescription())
                .coverImageUrl(request.getCoverImageUrl())
                .currency(request.getCurrency() != null ? request.getCurrency() : "XAF")
                .isAmountFixed(request.getIsAmountFixed() != null && request.getIsAmountFixed())
                .amount(request.getAmount())
                .expiresAt(request.getExpiresAt())
                .collectCustomerInfo(request.getCollectCustomerInfo() == null || request.getCollectCustomerInfo())
                .thankYouMessage(request.getThankYouMessage())
                .build();

        collection = fundCollectionRepository.save(collection);
        log.info("Collecte '{}' créée (slug: {}) pour l'application '{}' (user {})",
                collection.getTitle(), slug, application.getName(), userId);

        return toResponse(collection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundCollectionResponse> getCollections(UUID userId, UUID applicationId, FundCollectionStatus status) {
        List<FundCollection> collections;

        if (applicationId != null) {
            // Filtre par application
            collections = status != null
                    ? fundCollectionRepository.findByApplication_IdAndApplication_User_IdAndStatusOrderByCreatedAtDesc(
                            applicationId, userId, status)
                    : fundCollectionRepository.findByApplication_IdAndApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
                            applicationId, userId, FundCollectionStatus.DELETED);
        } else {
            // Toutes les collections de l'utilisateur
            collections = status != null
                    ? fundCollectionRepository.findByApplication_User_IdAndStatusOrderByCreatedAtDesc(userId, status)
                    : fundCollectionRepository.findByApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
                            userId, FundCollectionStatus.DELETED);
        }

        return collections.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FundCollectionResponse getCollection(UUID userId, UUID collectionId) {
        return toResponse(resolveCollection(userId, collectionId));
    }

    @Override
    @Transactional
    public FundCollectionResponse updateCollection(UUID userId, UUID collectionId, UpdateFundCollectionRequest request) {
        FundCollection collection = resolveCollection(userId, collectionId);

        if (collection.getStatus() != FundCollectionStatus.ACTIVE) {
            throw new BusinessException(
                    "Seules les collectes actives peuvent être modifiées. Statut actuel : " + collection.getStatus(),
                    HttpStatus.CONFLICT,
                    "COLLECTION_NOT_EDITABLE"
            );
        }

        boolean newIsAmountFixed = request.getIsAmountFixed() != null
                ? request.getIsAmountFixed()
                : collection.isAmountFixed();
        Long newAmount = request.getAmount() != null ? request.getAmount() : collection.getAmount();
        validateAmountConsistency(newIsAmountFixed, newAmount);

        if (request.getTitle() != null)               collection.setTitle(request.getTitle());
        if (request.getDescription() != null)         collection.setDescription(request.getDescription());
        if (request.getCoverImageUrl() != null)       collection.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getCurrency() != null)            collection.setCurrency(request.getCurrency());
        if (request.getIsAmountFixed() != null)       collection.setAmountFixed(request.getIsAmountFixed());
        if (request.getAmount() != null)              collection.setAmount(request.getAmount());
        if (Boolean.TRUE.equals(request.getRemoveExpiresAt())) {
            collection.setExpiresAt(null);
        } else if (request.getExpiresAt() != null) {
            collection.setExpiresAt(request.getExpiresAt());
        }
        if (request.getCollectCustomerInfo() != null) collection.setCollectCustomerInfo(request.getCollectCustomerInfo());
        if (request.getThankYouMessage() != null)     collection.setThankYouMessage(request.getThankYouMessage());

        collection = fundCollectionRepository.save(collection);
        log.info("Collecte '{}' mise à jour par l'utilisateur {}", collection.getTitle(), userId);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public FundCollectionResponse closeCollection(UUID userId, UUID collectionId) {
        FundCollection collection = resolveCollection(userId, collectionId);

        if (collection.getStatus() == FundCollectionStatus.CLOSED) {
            throw new BusinessException("La collecte est déjà clôturée.", HttpStatus.CONFLICT, "COLLECTION_ALREADY_CLOSED");
        }

        collection.setStatus(FundCollectionStatus.CLOSED);
        fundCollectionRepository.save(collection);
        log.info("Collecte '{}' clôturée par l'utilisateur {}", collection.getTitle(), userId);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public FundCollectionResponse reopenCollection(UUID userId, UUID collectionId) {
        FundCollection collection = resolveCollection(userId, collectionId);

        if (collection.getStatus() != FundCollectionStatus.CLOSED) {
            throw new BusinessException(
                    "Seules les collectes clôturées peuvent être réouvertes. Statut actuel : " + collection.getStatus(),
                    HttpStatus.CONFLICT,
                    "COLLECTION_NOT_CLOSED"
            );
        }

        if (collection.getExpiresAt() != null && collection.getExpiresAt().isBefore(OffsetDateTime.now())) {
            collection.setStatus(FundCollectionStatus.EXPIRED);
            collection = fundCollectionRepository.save(collection);
            log.info("Collecte '{}' marquée EXPIRED lors d'une tentative de réouverture (user {})", collection.getTitle(), userId);
            return toResponse(collection);
        }

        collection.setStatus(FundCollectionStatus.ACTIVE);
        fundCollectionRepository.save(collection);
        log.info("Collecte '{}' réouverte par l'utilisateur {}", collection.getTitle(), userId);

        return toResponse(collection);
    }

    @Override
    @Transactional
    public void deleteCollection(UUID userId, UUID collectionId) {
        FundCollection collection = resolveCollection(userId, collectionId);

        collection.setStatus(FundCollectionStatus.DELETED);
        fundCollectionRepository.save(collection);
        log.info("Collecte '{}' supprimée (soft delete) par l'utilisateur {}", collection.getTitle(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public FundCollectionResponse getPublicCollection(String slug) {
        FundCollection collection = fundCollectionRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException(
                        "Collecte introuvable.",
                        HttpStatus.NOT_FOUND,
                        "COLLECTION_NOT_FOUND"
                ));

        if (collection.getStatus() != FundCollectionStatus.ACTIVE) {
            throw new BusinessException(
                    "Cette collecte n'est plus disponible.",
                    HttpStatus.NOT_FOUND,
                    "COLLECTION_NOT_AVAILABLE"
            );
        }

        return toResponse(collection);
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private Application resolveApplication(UUID userId, UUID applicationId) {
        return applicationRepository
                .findByIdAndUserAndStatusNot(applicationId, userRepository.getReferenceById(userId), AppStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Application non trouvée ou accès non autorisé.",
                        HttpStatus.NOT_FOUND,
                        "APP_NOT_FOUND"
                ));
    }

    private FundCollection resolveCollection(UUID userId, UUID collectionId) {
        return fundCollectionRepository
                .findByIdAndApplication_User_IdAndStatusNot(collectionId, userId, FundCollectionStatus.DELETED)
                .orElseThrow(() -> new BusinessException(
                        "Collecte non trouvée.",
                        HttpStatus.NOT_FOUND,
                        "COLLECTION_NOT_FOUND"
                ));
    }

    private void validateAmountConsistency(Boolean isAmountFixed, Long amount) {
        if (Boolean.TRUE.equals(isAmountFixed) && (amount == null || amount <= 0)) {
            throw new BusinessException(
                    "Le montant est obligatoire et doit être supérieur à 0 lorsque isAmountFixed est true.",
                    HttpStatus.BAD_REQUEST,
                    "AMOUNT_REQUIRED_FOR_FIXED"
            );
        }
    }

    private String generateUniqueSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");

        if (base.length() > 80) base = base.substring(0, 80);
        if (base.isEmpty()) base = "collection";

        String candidate;
        do {
            byte[] bytes = new byte[3];
            secureRandom.nextBytes(bytes);
            candidate = base + "-" + HexFormat.of().formatHex(bytes);
        } while (fundCollectionRepository.existsBySlug(candidate));

        return candidate;
    }

    private FundCollectionResponse toResponse(FundCollection c) {
        return FundCollectionResponse.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .collectUrl(collectBaseUrl + c.getSlug())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .currency(c.getCurrency())
                .isAmountFixed(c.isAmountFixed())
                .amount(c.getAmount())
                .coverImageUrl(c.getCoverImageUrl())
                .collectCustomerInfo(c.isCollectCustomerInfo())
                .thankYouMessage(c.getThankYouMessage())
                .expiresAt(c.getExpiresAt())
                .applicationId(c.getApplication().getId())
                .applicationName(c.getApplication().getName())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
