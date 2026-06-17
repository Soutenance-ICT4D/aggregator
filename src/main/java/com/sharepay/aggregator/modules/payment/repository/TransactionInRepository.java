package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionInRepository extends JpaRepository<TransactionIn, UUID>, JpaSpecificationExecutor<TransactionIn> {

    Optional<TransactionIn> findByReference(String reference);

    Optional<TransactionIn> findByReferenceAndApplication_Id(String reference, UUID applicationId);

    Optional<TransactionIn> findBySessionToken(String sessionToken);

    Optional<TransactionIn> findByIdempotencyKey(String idempotencyKey);

    Optional<TransactionIn> findByProviderTransactionId(String providerTransactionId);

    List<TransactionIn> findByApplication_IdOrderByCreatedAtDesc(UUID applicationId);

    List<TransactionIn> findByApplication_IdAndStatusOrderByCreatedAtDesc(UUID applicationId, TransactionStatus status);

    List<TransactionIn> findByStatusAndProviderTransactionIdIsNotNull(TransactionStatus status);

    /**
     * Charge les transactions PENDING actives (non expirées) pour le polling du scheduler.
     * Les transactions expirées sont gérées séparément par findExpiredPending.
     */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND t.providerTransactionId IS NOT NULL
              AND (t.expiresAt IS NULL OR t.expiresAt > :now)
            """)
    List<TransactionIn> findPendingWithDetails(@Param("now") OffsetDateTime now);

    /** Recharge une transaction avec ses associations (utilisé par savePayInSuccess pour éviter les valeurs périmées). */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.id = :id
            """)
    Optional<TransactionIn> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Transactions PENDING à clôturer :
     *  - CHECKOUT / FUND_COLLECTION : expiresAt dépassé
     *  - CHARGE (pas d'expiresAt) : plus vieilles que maxAge (seuil = now - 2h)
     */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND (
                (t.expiresAt IS NOT NULL AND t.expiresAt < :now)
                OR
                (t.expiresAt IS NULL AND t.createdAt < :maxAge)
              )
            """)
    List<TransactionIn> findExpiredPending(
            @Param("now")    OffsetDateTime now,
            @Param("maxAge") OffsetDateTime maxAge
    );

    /**
     * Transactions PENDING sans providerTransactionId depuis plus de maxAge :
     * le gateway n'a jamais répondu, on ne peut pas les poller - on les expire tôt.
     * Exclut les transactions déjà expirées (gérées par findExpiredPending).
     */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND t.providerTransactionId IS NULL
              AND t.createdAt < :maxAge
              AND (t.expiresAt IS NULL OR t.expiresAt > :now)
            """)
    List<TransactionIn> findStuckPendingWithDetails(
            @Param("maxAge") OffsetDateTime maxAge,
            @Param("now")    OffsetDateTime now
    );

    /** Charge une transaction par providerTransactionId avec ses associations (utilisé par le callback provider). */
    @Query("""
            SELECT t FROM TransactionIn t
            JOIN FETCH t.application a
            JOIN FETCH a.user
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.providerTransactionId = :providerTransactionId
            """)
    Optional<TransactionIn> findByProviderTransactionIdWithDetails(@Param("providerTransactionId") String providerTransactionId);

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

    /** Toutes les transactions (vue admin globale), paginées. */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            JOIN FETCH t.application a
            JOIN FETCH a.user
            ORDER BY t.createdAt DESC
            """)
    Page<TransactionIn> findAllForAdmin(Pageable pageable);

    /** Toutes les transactions d'un marchand (vue admin), paginées. */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            JOIN FETCH t.application a
            JOIN FETCH a.user
            WHERE a.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<TransactionIn> findAllByMerchantForAdmin(@Param("userId") UUID userId, Pageable pageable);

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

    /** Liste paginée des transactions d'un marchand avec filtres optionnels sur le statut et le type. */
    @Query(
            value = """
                    SELECT t FROM TransactionIn t
                    LEFT JOIN FETCH t.paymentProvider
                    JOIN FETCH t.application a
                    WHERE a.user.id = :userId
                      AND a.status <> 'DELETED'
                      AND (:status IS NULL OR t.status = :status)
                      AND (:type IS NULL OR t.type = :type)
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM TransactionIn t
                    JOIN t.application a
                    WHERE a.user.id = :userId
                      AND a.status <> 'DELETED'
                      AND (:status IS NULL OR t.status = :status)
                      AND (:type IS NULL OR t.type = :type)
                    """
    )
    Page<TransactionIn> findPagedByUser(
            @Param("userId") UUID userId,
            @Param("status") TransactionStatus status,
            @Param("type") TransactionInType type,
            Pageable pageable
    );

    /** Nombre total de pay-in du marchand (apps non supprimées). */
    long countByApplication_User_IdAndApplication_StatusNot(UUID userId, AppStatus appStatus);

    /** Nombre de pay-in par statut pour le marchand (apps non supprimées). */
    long countByApplication_User_IdAndApplication_StatusNotAndStatus(UUID userId, AppStatus appStatus, TransactionStatus txStatus);

    /** Détail d'une transaction pay-in appartenant au marchand (vérification d'ownership). */
    @Query("""
            SELECT t FROM TransactionIn t
            LEFT JOIN FETCH t.paymentProvider
            JOIN FETCH t.application a
            WHERE t.id = :id
              AND a.user.id = :userId
              AND a.status <> 'DELETED'
            """)
    Optional<TransactionIn> findByIdForMerchant(@Param("id") UUID id, @Param("userId") UUID userId);
}
