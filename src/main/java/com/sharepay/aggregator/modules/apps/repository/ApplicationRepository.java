package com.sharepay.aggregator.modules.apps.repository;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.shared.constant.AppStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByUserAndStatusNotOrderByCreatedAtDesc(User user, AppStatus status);
    Optional<Application> findByIdAndUserAndStatusNot(UUID id, User user, AppStatus status);
    boolean existsByUserAndNameAndStatusNot(User user, String name, AppStatus status);
}
