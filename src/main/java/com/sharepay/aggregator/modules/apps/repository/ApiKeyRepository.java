package com.sharepay.aggregator.modules.apps.repository;

import com.sharepay.aggregator.modules.apps.model.ApiKey;
import com.sharepay.aggregator.modules.apps.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /** Toutes les clés d'une application, triées par création décroissante. */
    List<ApiKey> findByApplicationOrderByCreatedAtDesc(Application application);

    /** Clé active de l'application (une seule par application autorisée). */
    Optional<ApiKey> findByApplicationAndIsActiveTrue(Application application);

    /** Clés actives pour une liste d'applications — une seule requête SQL pour éviter le N+1. */
    List<ApiKey> findByApplicationInAndIsActiveTrue(List<Application> applications);

    /** Recherche par hash pour la validation lors des appels API. */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /** Recherche par préfixe (utilisé en complément du hash pour l'optimisation). */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);
}
