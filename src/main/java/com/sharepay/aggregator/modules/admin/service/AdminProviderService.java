package com.sharepay.aggregator.modules.admin.service;

import com.sharepay.aggregator.modules.admin.dto.request.UpsertProviderRequest;
import com.sharepay.aggregator.modules.admin.dto.response.AdminProviderResponse;

import java.util.List;
import java.util.UUID;

public interface AdminProviderService {
    List<AdminProviderResponse> listProviders();
    AdminProviderResponse createProvider(UpsertProviderRequest request);
    AdminProviderResponse updateProvider(UUID providerId, UpsertProviderRequest request);
    AdminProviderResponse toggleStatus(UUID providerId);
}
