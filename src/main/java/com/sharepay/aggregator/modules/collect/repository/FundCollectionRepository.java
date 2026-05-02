package com.sharepay.aggregator.modules.collect.repository;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundCollectionRepository extends JpaRepository<FundCollection, UUID> {

    // ── Requêtes toutes apps confondues (niveau utilisateur) ──────────────────

    @Query("""
            SELECT fc FROM FundCollection fc
            WHERE fc.application.user.id = :userId
              AND fc.status != :excludedStatus
              AND fc.application.status != 'DELETED'
            ORDER BY fc.createdAt DESC
            """)
    List<FundCollection> findByApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
            @Param("userId") UUID userId, @Param("excludedStatus") FundCollectionStatus excludedStatus);

    @Query("""
            SELECT fc FROM FundCollection fc
            WHERE fc.application.user.id = :userId
              AND fc.status = :status
              AND fc.application.status != 'DELETED'
            ORDER BY fc.createdAt DESC
            """)
    List<FundCollection> findByApplication_User_IdAndStatusOrderByCreatedAtDesc(
            @Param("userId") UUID userId, @Param("status") FundCollectionStatus status);

    // ── Requêtes filtrées par application ─────────────────────────────────────

    @Query("""
            SELECT fc FROM FundCollection fc
            WHERE fc.application.id = :appId
              AND fc.application.user.id = :userId
              AND fc.status != :excludedStatus
              AND fc.application.status != 'DELETED'
            ORDER BY fc.createdAt DESC
            """)
    List<FundCollection> findByApplication_IdAndApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
            @Param("appId") UUID appId, @Param("userId") UUID userId, @Param("excludedStatus") FundCollectionStatus excludedStatus);

    @Query("""
            SELECT fc FROM FundCollection fc
            WHERE fc.application.id = :appId
              AND fc.application.user.id = :userId
              AND fc.status = :status
              AND fc.application.status != 'DELETED'
            ORDER BY fc.createdAt DESC
            """)
    List<FundCollection> findByApplication_IdAndApplication_User_IdAndStatusOrderByCreatedAtDesc(
            @Param("appId") UUID appId, @Param("userId") UUID userId, @Param("status") FundCollectionStatus status);

    // ── Suppression en cascade depuis l'application ───────────────────────────

    @Modifying
    @Query("UPDATE FundCollection fc SET fc.status = 'DELETED' WHERE fc.application.id = :appId AND fc.status != 'DELETED'")
    void softDeleteByApplicationId(@Param("appId") UUID appId);

    // ── Requête pour les opérations individuelles (get, update, close…) ───────

    Optional<FundCollection> findByIdAndApplication_User_IdAndStatusNot(
            UUID id, UUID userId, FundCollectionStatus excludedStatus);

    // ── Expiration ────────────────────────────────────────────────────────────

    List<FundCollection> findByStatusAndExpiresAtBefore(FundCollectionStatus status, OffsetDateTime now);

    // ── Slug ──────────────────────────────────────────────────────────────────

    boolean existsBySlug(String slug);

    Optional<FundCollection> findBySlug(String slug);
}
