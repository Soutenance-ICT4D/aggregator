package com.sharepay.aggregator.modules.apps.service;

import com.sharepay.aggregator.modules.apps.dto.request.CreateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.request.UpdateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApplicationResponse;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {
    ApplicationResponse createApplication(UUID userId, CreateApplicationRequest request);
    List<ApplicationResponse> getApplications(UUID userId);
    ApplicationResponse getApplication(UUID userId, UUID appId);
    ApplicationResponse updateApplication(UUID userId, UUID appId, UpdateApplicationRequest request);
    void deleteApplication(UUID userId, UUID appId);
}
