package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionOutRepository extends JpaRepository<TransactionOut, UUID> {

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
    List<TransactionOut> findStuckPendingWithDetails(@org.springframework.data.repository.query.Param("maxAge") java.time.OffsetDateTime maxAge);
}
