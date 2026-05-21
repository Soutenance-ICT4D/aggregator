package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.response.AdminTransactionResponse;
import com.sharepay.aggregator.modules.admin.service.AdminTransactionService;
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
@RequestMapping("/api/v1/admin/transactions")
@Tag(name = "Admin Transactions", description = "Vue globale des transactions pay-in")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    @GetMapping
    @Operation(summary = "Toutes les transactions (tous marchands confondus)")
    public ApiResponse<PaginationResponse<AdminTransactionResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminTransactionService.listTransactions(page, size));
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Transactions d'un marchand spécifique")
    public ApiResponse<PaginationResponse<AdminTransactionResponse>> listByMerchant(
            @PathVariable UUID merchantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminTransactionService.listTransactionsByMerchant(merchantId, page, size));
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Détail d'une transaction par référence")
    public ApiResponse<AdminTransactionResponse> getByReference(@PathVariable String reference) {
        return ApiResponse.success(adminTransactionService.getByReference(reference));
    }
}
