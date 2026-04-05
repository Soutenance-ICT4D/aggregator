package com.sharepay.aggregator.modules.collect.repository;

import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundCollectionRepository extends JpaRepository<FundCollection, UUID> {

    // ── Requêtes toutes apps confondues (niveau utilisateur) ──────────────────

    List<FundCollection> findByApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
            UUID userId, FundCollectionStatus excludedStatus);

    List<FundCollection> findByApplication_User_IdAndStatusOrderByCreatedAtDesc(
            UUID userId, FundCollectionStatus status);

    // ── Requêtes filtrées par application ─────────────────────────────────────

    List<FundCollection> findByApplication_IdAndApplication_User_IdAndStatusNotOrderByCreatedAtDesc(
            UUID appId, UUID userId, FundCollectionStatus excludedStatus);

    List<FundCollection> findByApplication_IdAndApplication_User_IdAndStatusOrderByCreatedAtDesc(
            UUID appId, UUID userId, FundCollectionStatus status);

    // ── Requête pour les opérations individuelles (get, update, close…) ───────

    Optional<FundCollection> findByIdAndApplication_User_IdAndStatusNot(
            UUID id, UUID userId, FundCollectionStatus excludedStatus);

    // ── Expiration ────────────────────────────────────────────────────────────

    List<FundCollection> findByStatusAndExpiresAtBefore(FundCollectionStatus status, OffsetDateTime now);

    // ── Slug ──────────────────────────────────────────────────────────────────

    boolean existsBySlug(String slug);

    Optional<FundCollection> findBySlug(String slug);
}
