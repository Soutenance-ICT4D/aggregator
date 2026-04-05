package com.sharepay.aggregator.modules.collect.service;

import com.sharepay.aggregator.modules.collect.dto.request.CreateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.request.UpdateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.response.FundCollectionResponse;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;

import java.util.List;
import java.util.UUID;

public interface FundCollectionService {

    /** Crée une collecte pour l'application spécifiée. */
    FundCollectionResponse createCollection(UUID userId, UUID applicationId, CreateFundCollectionRequest request);

    /**
     * Liste les collectes de l'utilisateur.
     * Si applicationId est fourni, filtre par application.
     * Si status est fourni, filtre par statut exact — sinon exclut les DELETED.
     */
    List<FundCollectionResponse> getCollections(UUID userId, UUID applicationId, FundCollectionStatus status);

    /** Retourne le détail d'une collecte appartenant à l'utilisateur. */
    FundCollectionResponse getCollection(UUID userId, UUID collectionId);

    /** Mise à jour partielle d'une collecte (ACTIVE uniquement). */
    FundCollectionResponse updateCollection(UUID userId, UUID collectionId, UpdateFundCollectionRequest request);

    /** Clôture une collecte (ACTIVE ou EXPIRED → CLOSED). */
    FundCollectionResponse closeCollection(UUID userId, UUID collectionId);

    /** Réouvre une collecte clôturée (CLOSED → ACTIVE, si pas expirée). */
    FundCollectionResponse reopenCollection(UUID userId, UUID collectionId);

    /** Soft delete d'une collecte. */
    void deleteCollection(UUID userId, UUID collectionId);

    /** Retourne une collecte active par son slug (accès public, sans authentification). */
    FundCollectionResponse getPublicCollection(String slug);
}
