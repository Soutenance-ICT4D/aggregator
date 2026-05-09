package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateMerchantKycRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.MerchantSummaryResponse;
import com.sharepay.aggregator.modules.admin.service.AdminMerchantService;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMerchantServiceImpl implements AdminMerchantService {

    private final UserRepository userRepository;

    @Override
    public PaginationResponse<MerchantSummaryResponse> listMerchants(int page, int size, AccountStatus status) {
        Page<User> result = (status != null)
                ? userRepository.findAllByRoleAndStatusOrderByCreatedAtDesc(Role.MERCHANT, status, PageRequest.of(page, size))
                : userRepository.findAllByRoleOrderByCreatedAtDesc(Role.MERCHANT, PageRequest.of(page, size));
        return toPaginationResponse(result);
    }

    @Override
    public MerchantSummaryResponse getMerchant(UUID merchantId) {
        return toResponse(resolveMerchant(merchantId));
    }

    @Override
    @Transactional
    public MerchantSummaryResponse updateStatus(UUID merchantId, UpdateStatusRequest request) {
        User merchant = resolveMerchant(merchantId);
        merchant.setStatus(request.getStatus());
        return toResponse(userRepository.save(merchant));
    }

    @Override
    @Transactional
    public MerchantSummaryResponse updateKyc(UUID merchantId, UpdateMerchantKycRequest request) {
        User merchant = resolveMerchant(merchantId);
        merchant.setKycLevel(request.getKycLevel());
        return toResponse(userRepository.save(merchant));
    }

    private User resolveMerchant(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Marchand introuvable.", HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND"));
        if (user.getRole() != Role.MERCHANT) {
            throw new BusinessException("Cet utilisateur n'est pas un marchand.", HttpStatus.BAD_REQUEST, "NOT_MERCHANT");
        }
        return user;
    }

    private MerchantSummaryResponse toResponse(User user) {
        return MerchantSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .country(user.getCountry())
                .status(user.getStatus())
                .kycLevel(user.getKycLevel())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PaginationResponse<MerchantSummaryResponse> toPaginationResponse(Page<User> page) {
        return PaginationResponse.<MerchantSummaryResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
