package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.response.MerchantDashboardResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.UserBalanceResponse;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants")
@Tag(name = "Compte Marchand", description = "Endpoints de gestion du compte marchand")
@SecurityRequirement(name = "bearerAuth")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/balance")
    @Operation(
            summary = "Consulter son solde",
            description = "Retourne les soldes du marchand par devise. " +
                    "`availableAmount` est le montant disponible pour un virement. " +
                    "`pendingAmount` correspond aux payouts en cours de traitement."
    )
    public ApiResponse<List<UserBalanceResponse>> getBalance(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(merchantService.getBalances(currentUser.getAccountId()));
    }

    @GetMapping("/transactions/chart")
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

    @GetMapping("/dashboard")
    @Operation(
            summary = "Tableau de bord marchand",
            description = """
                    Retourne un résumé complet du tableau de bord du marchand :
                    - `availableBalance` : solde disponible pour un virement (en plus petite unité monétaire)
                    - `pendingBalance` : solde en attente (payout en cours)
                    - `dailyVolume` : volume total encaissé aujourd'hui (transactions SUCCESS uniquement)
                    - `todayTransactionCount` : nombre de transactions du jour
                    - `todayTransactions` : liste complète des transactions du jour
                    - `lastFiveTransactions` : les 5 dernières transactions toutes dates confondues
                    """
    )
    public ApiResponse<MerchantDashboardResponse> getDashboard(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(merchantService.getDashboard(currentUser.getAccountId()));
    }
}
