package com.sharepay.aggregator.modules.webhook.repository;

import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import com.sharepay.aggregator.modules.webhook.model.WebhookDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    /** Livraisons éligibles au retry : FAILED, sous le seuil max et dont le délai est écoulé.
     *  JOIN FETCH sur application : évite LazyInitializationException hors transaction dans le scheduler. */
    @Query("""
            SELECT d FROM WebhookDelivery d
            JOIN FETCH d.application
            WHERE d.status = 'FAILED'
              AND d.attemptCount < :maxAttempts
              AND d.nextRetryAt <= :now
            ORDER BY d.nextRetryAt ASC
            """)
    List<WebhookDelivery> findRetryable(@Param("now") OffsetDateTime now,
                                        @Param("maxAttempts") int maxAttempts);

    /** Historique paginé pour le dashboard merchant, filtrable par statut. */
    Page<WebhookDelivery> findByApplication_IdOrderByCreatedAtDesc(UUID applicationId, Pageable pageable);

    Page<WebhookDelivery> findByApplication_IdAndStatusOrderByCreatedAtDesc(
            UUID applicationId, WebhookDeliveryStatus status, Pageable pageable);

    /** Lookup sécurisé pour le replay : vérifie l'appartenance à l'application. */
    Optional<WebhookDelivery> findByIdAndApplication_Id(UUID id, UUID applicationId);
}
