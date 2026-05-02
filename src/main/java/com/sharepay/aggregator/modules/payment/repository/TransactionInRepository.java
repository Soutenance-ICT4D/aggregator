package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionInRepository extends JpaRepository<TransactionIn, UUID> {

    Optional<TransactionIn> findByReference(String reference);

    Optional<TransactionIn> findByReferenceAndApplication_Id(String reference, UUID applicationId);

    Optional<TransactionIn> findBySessionToken(String sessionToken);

    Optional<TransactionIn> findByIdempotencyKey(String idempotencyKey);

    Optional<TransactionIn> findByProviderTransactionId(String providerTransactionId);

    List<TransactionIn> findByApplication_IdOrderByCreatedAtDesc(UUID applicationId);

    List<TransactionIn> findByApplication_IdAndStatusOrderByCreatedAtDesc(UUID applicationId, TransactionStatus status);

    List<TransactionIn> findByStatusAndProviderTransactionIdIsNotNull(TransactionStatus status);

    /** Charge les transactions PENDING avec toutes les associations nécessaires au scheduler. */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND t.providerTransactionId IS NOT NULL
            """)
    List<TransactionIn> findPendingWithDetails();

    /** Charge une transaction avec son application pour la page checkout. */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application
            WHERE t.sessionToken = :sessionToken
            """)
    Optional<TransactionIn> findBySessionTokenWithApplication(String sessionToken);

    /** Charge une transaction avec ses associations pour la génération du reçu. */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.reference = :reference
            """)
    Optional<TransactionIn> findByReferenceWithDetails(String reference);

    /** Volume journalier : somme des netAmount des transactions SUCCESS du marchand pour aujourd'hui (apps non supprimées). */
    @Query("""
            SELECT COALESCE(SUM(t.netAmount), 0)
            FROM TransactionIn t
            WHERE t.application.user.id = :userId
              AND t.application.status != 'DELETED'
              AND t.status = 'SUCCESS'
              AND t.createdAt >= :startOfDay
              AND t.createdAt < :endOfDay
            """)
    Long sumDailyVolume(@Param("userId") UUID userId,
                        @Param("startOfDay") OffsetDateTime startOfDay,
                        @Param("endOfDay") OffsetDateTime endOfDay);

    /** Transactions du jour pour toutes les applications actives/suspendues du marchand, ordre anti-chronologique. */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.application.user.id = :userId
              AND t.application.status != 'DELETED'
              AND t.createdAt >= :startOfDay
              AND t.createdAt < :endOfDay
            ORDER BY t.createdAt DESC
            """)
    List<TransactionIn> findTodayByUser(@Param("userId") UUID userId,
                                        @Param("startOfDay") OffsetDateTime startOfDay,
                                        @Param("endOfDay") OffsetDateTime endOfDay);

    /** Dernières transactions du marchand (apps non supprimées), paginées. */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.application.user.id = :userId
              AND t.application.status != 'DELETED'
            ORDER BY t.createdAt DESC
            """)
    List<TransactionIn> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    /** Toutes les transactions du marchand (apps non supprimées) dans un intervalle, avec provider et application chargés. */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            JOIN FETCH t.application a
            WHERE a.user.id = :userId
              AND a.status != 'DELETED'
              AND t.createdAt >= :from
              AND t.createdAt < :to
            ORDER BY t.createdAt ASC
            """)
    List<TransactionIn> findForChartByUser(@Param("userId") UUID userId,
                                           @Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    /** Noms distincts des applications non supprimées de ce marchand. */
    @Query("""
            SELECT DISTINCT a.name
            FROM TransactionIn t
            JOIN t.application a
            WHERE a.user.id = :userId
              AND a.status != 'DELETED'
            ORDER BY a.name ASC
            """)
    List<String> findDistinctApplicationNamesByUser(@Param("userId") UUID userId);
}
