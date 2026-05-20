package com.sharepay.aggregator.modules.account.repository;

import com.sharepay.aggregator.modules.account.model.WithdrawalConfig;
import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalConfigRepository extends JpaRepository<WithdrawalConfig, UUID> {

    @Query("SELECT c FROM WithdrawalConfig c JOIN FETCH c.user LEFT JOIN FETCH c.account WHERE c.user.id = :userId")
    Optional<WithdrawalConfig> findByUserId(UUID userId);

    @Query("SELECT c FROM WithdrawalConfig c JOIN FETCH c.user JOIN FETCH c.account WHERE c.mode = :mode AND c.account IS NOT NULL AND c.account.deletedAt IS NULL")
    List<WithdrawalConfig> findActiveByMode(WithdrawalMode mode);
}
