package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.request.UpdateMerchantKycRequest;
import com.sharepay.aggregator.modules.admin.dto.request.UpdateStatusRequest;
import com.sharepay.aggregator.modules.admin.dto.response.MerchantSummaryResponse;
import com.sharepay.aggregator.modules.admin.service.AdminMerchantService;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/merchants")
@Tag(name = "Admin — Marchands", description = "Gestion des comptes marchands")
@SecurityRequirement(name = "bearerAuth")
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    @GetMapping
    @Operation(summary = "Lister les marchands", description = "Filtre optionnel par statut.")
    public ApiResponse<PaginationResponse<MerchantSummaryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AccountStatus status
    ) {
        return ApiResponse.success(adminMerchantService.listMerchants(page, size, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un marchand")
    public ApiResponse<MerchantSummaryResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(adminMerchantService.getMerchant(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activer, suspendre ou supprimer un marchand")
    public ApiResponse<MerchantSummaryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ApiResponse.success(adminMerchantService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/kyc")
    @Operation(summary = "Changer le niveau KYC d'un marchand")
    public ApiResponse<MerchantSummaryResponse> updateKyc(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantKycRequest request
    ) {
        return ApiResponse.success(adminMerchantService.updateKyc(id, request));
    }
}
