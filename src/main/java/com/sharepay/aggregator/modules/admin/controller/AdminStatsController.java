package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.response.AdminOverviewResponse;
import com.sharepay.aggregator.modules.admin.service.AdminStatsService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stats")
@Tag(name = "Statistiques admin", description = "Vue d'ensemble plateforme")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/overview")
    @Operation(summary = "Vue d'ensemble plateforme",
            description = "KPIs agrégés, file de vérification KYB, derniers marchands et transactions.")
    public ApiResponse<AdminOverviewResponse> overview() {
        return ApiResponse.success(adminStatsService.getOverview());
    }
}
