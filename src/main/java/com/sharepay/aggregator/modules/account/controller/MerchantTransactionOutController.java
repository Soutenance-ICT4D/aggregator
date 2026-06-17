package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.response.TransactionOutDetailResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionOutSummaryResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionStatsResponse;
import com.sharepay.aggregator.modules.account.dto.response.TxChartResponse;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.constant.TxChartCustomType;
import com.sharepay.aggregator.shared.constant.TxChartInterval;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants/transactions-out")
@Tag(name = "Transactions sortantes (Pay-out)", description = "Consultation des retraits du marchand connecté")
@SecurityRequirement(name = "bearerAuth")
public class MerchantTransactionOutController {

    private final MerchantService merchantService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales des transactions sortantes")
    public ApiResponse<TransactionStatsResponse> stats(
            @AuthenticationPrincipal AuthentificatedUser currentUser) {
        return ApiResponse.success(merchantService.getTransactionOutStats(currentUser.getAccountId()));
    }

    @GetMapping
    @Operation(
            summary = "Liste paginée des transactions sortantes",
            description = """
                    Retourne les retraits (pay-out) du marchand connecté, paginés et filtrables.
                    - `page`   : numéro de page (0-based, défaut : 0)
                    - `size`   : nombre d'éléments par page (défaut : 20, max : 100)
                    - `status` : filtre optionnel - `PENDING` | `SUCCESS` | `FAILED` | `CANCELLED` | `REFUNDED`
                    - `appId`  : filtre optionnel - UUID de l'application
                    - `from`   : date de début inclusive (format ISO : `2025-01-01`)
                    - `to`     : date de fin inclusive (format ISO : `2025-01-31`)
                    """
    )
    public ApiResponse<PaginationResponse<TransactionOutSummaryResponse>> list(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) UUID appId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        OffsetDateTime fromDt = from != null ? from.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        return ApiResponse.success(
                merchantService.getTransactionsOut(currentUser.getAccountId(), page, size, status, appId, fromDt, toDt)
        );
    }

    @GetMapping("/chart")
    @Operation(
            summary = "Graphique des transactions sortantes",
            description = """
                    Retourne des séries temporelles de retraits groupés par un critère.

                    **Intervalles :**
                    - `TODAY`      : aujourd'hui, par heure (24 slots)
                    - `THIS_WEEK`  : semaine en cours (lundi → dimanche), par jour
                    - `THIS_MONTH` : mois en cours, par jour
                    - `CUSTOM`     : personnalisé, nécessite `customType`

                    **Pour `interval=CUSTOM` :**
                    - `customType=YEAR` + `year=2025`          : année complète, par mois (12 slots)
                    - `customType=DAYS` + `from=...&to=...`    : plage libre, max 30 jours, par jour

                    **groupBy :** `STATUS` | `PROVIDER` | `APPLICATION`
                    """
    )
    public ApiResponse<TxChartResponse> chart(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @RequestParam(defaultValue = "THIS_WEEK") TxChartInterval interval,
            @RequestParam(required = false) TxChartCustomType customType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "STATUS") ChartGroupBy groupBy
    ) {
        return ApiResponse.success(
                merchantService.getTransactionOutChart(
                        currentUser.getAccountId(), interval, customType, year, from, to, groupBy)
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Détail d'une transaction sortante",
            description = "Retourne tous les champs d'un retrait appartenant au marchand connecté."
    )
    public ApiResponse<TransactionOutDetailResponse> detail(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(
                merchantService.getTransactionOutDetail(currentUser.getAccountId(), id)
        );
    }
}
