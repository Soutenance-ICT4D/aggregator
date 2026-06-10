package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionSummaryResponse;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants/transactions")
@Tag(name = "Transactions Marchand", description = "Consultation des transactions du marchand connecté")
@SecurityRequirement(name = "bearerAuth")
public class MerchantTransactionController {

    private final MerchantService merchantService;

    @GetMapping
    @Operation(
            summary = "Liste paginée des transactions",
            description = """
                    Retourne les transactions pay-in du marchand connecté, paginées et filtrables.
                    - `page`   : numéro de page (0-based, défaut : 0)
                    - `size`   : nombre d'éléments par page (défaut : 20, max recommandé : 100)
                    - `status` : filtre optionnel - `PENDING` | `SUCCESS` | `FAILED` | `CANCELLED` | `REFUNDED`
                    - `type`   : filtre optionnel - `CHECKOUT` | `CHARGE` | `FUND_COLLECTION`
                    """
    )
    public ApiResponse<PaginationResponse<TransactionSummaryResponse>> getTransactions(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionInType type
    ) {
        return ApiResponse.success(
                merchantService.getTransactions(currentUser.getAccountId(), page, size, status, type)
        );
    }

    @GetMapping("/chart")
    @Operation(
            summary = "Données du graphique de transactions",
            description = """
                    Retourne des séries temporelles de transactions groupées par un critère.
                    - `interval` : `TODAY` (par heure) | `LAST_7_DAYS` | `LAST_30_DAYS` (par jour)
                    - `groupBy`  : `STATUS` | `PROVIDER` | `APPLICATION`
                    Chaque série contient un tableau `counts` et `volumes` alignés sur `labels`.
                    """
    )
    public ApiResponse<TransactionChartResponse> getTransactionChart(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @RequestParam(defaultValue = "LAST_7_DAYS") ChartInterval interval,
            @RequestParam(defaultValue = "STATUS") ChartGroupBy groupBy
    ) {
        return ApiResponse.success(
                merchantService.getTransactionChart(currentUser.getAccountId(), interval, groupBy)
        );
    }
}
