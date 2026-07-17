package com.sharepay.aggregator.modules.admin.service;

import com.sharepay.aggregator.modules.admin.dto.response.AdminOverviewResponse;

public interface AdminStatsService {
    AdminOverviewResponse getOverview();
}
