package com.sharepay.aggregator.modules.admin.service.impl;

import com.sharepay.aggregator.modules.admin.dto.request.UpsertProviderRequest;
import com.sharepay.aggregator.modules.admin.dto.response.AdminProviderResponse;
import com.sharepay.aggregator.modules.admin.service.AdminProviderService;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProviderServiceImpl implements AdminProviderService {

    private final PaymentProviderRepository paymentProviderRepository;

    @Override
    public List<AdminProviderResponse> listProviders() {
        return paymentProviderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminProviderResponse createProvider(UpsertProviderRequest request) {
        if (paymentProviderRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessException("Un provider avec ce code existe déjà.", HttpStatus.CONFLICT, "PROVIDER_CODE_EXISTS");
        }
        PaymentProvider provider = PaymentProvider.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .country(request.getCountry())
                .currency(request.getCurrency())
                .feePercentage(request.getFeePercentage())
                .feeFixed(request.getFeeFixed())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .isActive(true)
                .build();
        return toResponse(paymentProviderRepository.save(provider));
    }

    @Override
    @Transactional
    public AdminProviderResponse updateProvider(UUID providerId, UpsertProviderRequest request) {
        PaymentProvider provider = resolve(providerId);
        provider.setName(request.getName());
        provider.setType(request.getType());
        provider.setCountry(request.getCountry());
        provider.setCurrency(request.getCurrency());
        provider.setFeePercentage(request.getFeePercentage());
        provider.setFeeFixed(request.getFeeFixed());
        provider.setMinAmount(request.getMinAmount());
        provider.setMaxAmount(request.getMaxAmount());
        return toResponse(paymentProviderRepository.save(provider));
    }

    @Override
    @Transactional
    public AdminProviderResponse toggleStatus(UUID providerId) {
        PaymentProvider provider = resolve(providerId);
        provider.setActive(!provider.isActive());
        return toResponse(paymentProviderRepository.save(provider));
    }

    private PaymentProvider resolve(UUID id) {
        return paymentProviderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Provider introuvable.", HttpStatus.NOT_FOUND, "PROVIDER_NOT_FOUND"));
    }

    private AdminProviderResponse toResponse(PaymentProvider p) {
        return AdminProviderResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .type(p.getType())
                .country(p.getCountry())
                .currency(p.getCurrency())
                .active(p.isActive())
                .feePercentage(p.getFeePercentage())
                .feeFixed(p.getFeeFixed())
                .minAmount(p.getMinAmount())
                .maxAmount(p.getMaxAmount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
