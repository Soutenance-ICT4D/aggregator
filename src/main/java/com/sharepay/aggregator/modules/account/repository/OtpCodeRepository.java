package com.sharepay.aggregator.modules.account.repository;

import com.sharepay.aggregator.modules.account.model.OtpCode;
import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.shared.constant.OtpPurpose;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    void deleteByUser(User user);
    void deleteByUserAndPurpose(User user, OtpPurpose purpose);
    Optional<OtpCode> findFirstByUserAndPurposeOrderByCreatedAtDesc(User user, OtpPurpose purpose);
}

