package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
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
public interface TransactionOutRepository extends JpaRepository<TransactionOut, UUID>, JpaSpecificationExecutor<TransactionOut> {

    Optional<TransactionOut> findByReference(String reference);

    Optional<TransactionOut> findByReferenceAndApplication_Id(String reference, UUID applicationId);

    Optional<TransactionOut> findByProviderTransactionId(String providerTransactionId);

    List<TransactionOut> findByApplication_IdOrderByCreatedAtDesc(UUID applicationId);

    List<TransactionOut> findByApplication_IdAndStatusOrderByCreatedAtDesc(UUID applicationId, TransactionStatus status);

    List<TransactionOut> findByStatusAndProviderTransactionIdIsNotNull(TransactionStatus status);

    /** Charge les transactions PENDING avec toutes les associations nécessaires au scheduler. */
    @Query("""
            SELECT t FROM TransactionOut t
            JOIN FETCH t.user
            LEFT JOIN FETCH t.application
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND t.providerTransactionId IS NOT NULL
            """)
    List<TransactionOut> findPendingWithDetails();

    /** Charge les transactions PENDING sans providerTransactionId depuis plus de :maxAge (gateway non confirmé). */
    @Query("""
            SELECT t FROM TransactionOut t
            JOIN FETCH t.user
            LEFT JOIN FETCH t.application
            LEFT JOIN FETCH t.paymentProvider
            WHERE t.status = 'PENDING'
              AND t.providerTransactionId IS NULL
              AND t.createdAt < :maxAge
            """)
    List<TransactionOut> findStuckPendingWithDetails(@Param("maxAge") OffsetDateTime maxAge);

    /** Nombre total de pay-out du marchand. */
    long countByUser_Id(UUID userId);

    /** Nombre de pay-out par statut pour le marchand. */
    long countByUser_IdAndStatus(UUID userId, TransactionStatus status);

    /** Détail d'un pay-out appartenant au marchand (vérification d'ownership). */
    @Query("""
            SELECT t FROM TransactionOut t
            LEFT JOIN FETCH t.paymentProvider
            LEFT JOIN FETCH t.application
            WHERE t.id = :id
              AND t.user.id = :userId
            """)
    Optional<TransactionOut> findByIdForMerchant(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Toutes les transactions pay-out du marchand dans un intervalle, pour le graphique. */
    @Query("""
            SELECT t FROM TransactionOut t
            LEFT JOIN FETCH t.paymentProvider
            LEFT JOIN FETCH t.application
            WHERE t.user.id = :userId
              AND t.createdAt >= :from
              AND t.createdAt < :to
            ORDER BY t.createdAt ASC
            """)
    List<TransactionOut> findForChartByUser(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    /** Noms distincts des applications liées aux pay-out du marchand. */
    @Query("""
            SELECT DISTINCT a.name
            FROM TransactionOut t
            JOIN t.application a
            WHERE t.user.id = :userId
            ORDER BY a.name ASC
            """)
    List<String> findDistinctApplicationNamesByUser(@Param("userId") UUID userId);
}
