package com.sharepay.aggregator.modules.account.repository;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    long countByRoleAndStatus(Role role, AccountStatus status);

    Page<User> findAllByRoleOrderByCreatedAtDesc(Role role, Pageable pageable);
    Page<User> findAllByRoleAndStatusOrderByCreatedAtDesc(Role role, AccountStatus status, Pageable pageable);
    Page<User> findAllByRoleInOrderByCreatedAtDesc(List<Role> roles, Pageable pageable);
    List<User> findAllByRoleIn(List<Role> roles);
}
