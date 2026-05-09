package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.admin.dto.request.CreateStaffRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.StaffResponse;
import com.sharepay.aggregator.modules.admin.service.AdminStaffService;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStaffServiceImpl implements AdminStaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        if (request.getRole() == Role.MERCHANT) {
            throw new BusinessException("Impossible de créer un compte MERCHANT via cet endpoint.",
                    HttpStatus.BAD_REQUEST, "INVALID_ROLE");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Cet email est déjà utilisé.", HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
        }

        User staff = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .kycLevel(KycLevel.VERIFIED)
                .emailVerified(true)
                .build();

        return toResponse(userRepository.save(staff));
    }

    @Override
    public PaginationResponse<StaffResponse> listStaff(int page, int size) {
        Page<User> result = userRepository.findAllByRoleInOrderByCreatedAtDesc(
                List.of(Role.ADMIN, Role.SUPPORT),
                PageRequest.of(page, size)
        );
        return toPaginationResponse(result);
    }

    @Override
    @Transactional
    public StaffResponse updateStatus(UUID staffId, UpdateStatusRequest request) {
        User staff = resolveStaff(staffId);
        staff.setStatus(request.getStatus());
        return toResponse(userRepository.save(staff));
    }

    @Override
    @Transactional
    public void deleteStaff(UUID staffId) {
        User staff = resolveStaff(staffId);
        staff.setStatus(AccountStatus.DELETED);
        userRepository.save(staff);
    }

    private User resolveStaff(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Compte introuvable.", HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND"));
        if (user.getRole() == Role.MERCHANT) {
            throw new BusinessException("Cet utilisateur n'est pas un membre du staff.", HttpStatus.BAD_REQUEST, "NOT_STAFF");
        }
        return user;
    }

    private StaffResponse toResponse(User user) {
        return StaffResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PaginationResponse<StaffResponse> toPaginationResponse(Page<User> page) {
        return PaginationResponse.<StaffResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
