package com.sharepay.aggregator.modules.account.repository;

import com.sharepay.aggregator.modules.account.model.WithdrawalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WithdrawalAccountRepository extends JpaRepository<WithdrawalAccount, UUID> {

    List<WithdrawalAccount> findByUser_IdAndDeletedAtIsNullOrderByIsDefaultDescCreatedAtAsc(UUID userId);

    Optional<WithdrawalAccount> findByIdAndUser_IdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<WithdrawalAccount> findByUser_IdAndIsDefaultTrueAndDeletedAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE WithdrawalAccount a SET a.isDefault = false WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
