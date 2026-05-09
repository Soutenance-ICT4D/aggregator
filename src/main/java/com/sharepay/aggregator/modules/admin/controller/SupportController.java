package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.response.AdminTransactionResponse;
import com.sharepay.aggregator.modules.admin.dto.response.MerchantSummaryResponse;
import com.sharepay.aggregator.modules.admin.service.AdminMerchantService;
import com.sharepay.aggregator.modules.admin.service.AdminTransactionService;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/support")
@Tag(name = "Support", description = "Accès en lecture seule pour les agents de support")
@SecurityRequirement(name = "bearerAuth")
public class SupportController {

    private final AdminMerchantService adminMerchantService;
    private final AdminTransactionService adminTransactionService;

    @GetMapping("/merchants")
    @Operation(summary = "Lister les marchands")
    public ApiResponse<PaginationResponse<MerchantSummaryResponse>> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AccountStatus status
    ) {
        return ApiResponse.success(adminMerchantService.listMerchants(page, size, status));
    }

    @GetMapping("/merchants/{id}")
    @Operation(summary = "Détail d'un marchand")
    public ApiResponse<MerchantSummaryResponse> getMerchant(@PathVariable UUID id) {
        return ApiResponse.success(adminMerchantService.getMerchant(id));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Toutes les transactions")
    public ApiResponse<PaginationResponse<AdminTransactionResponse>> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminTransactionService.listTransactions(page, size));
    }

    @GetMapping("/transactions/{reference}")
    @Operation(summary = "Détail d'une transaction par référence")
    public ApiResponse<AdminTransactionResponse> getTransaction(@PathVariable String reference) {
        return ApiResponse.success(adminTransactionService.getByReference(reference));
    }
}
