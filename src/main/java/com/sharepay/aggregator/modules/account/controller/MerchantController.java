package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.request.ChangePasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateProfileRequest;
import com.sharepay.aggregator.modules.account.dto.response.MerchantDashboardResponse;
import com.sharepay.aggregator.modules.account.dto.response.MerchantProfileResponse;
import com.sharepay.aggregator.modules.account.dto.response.PaymentProviderResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionChartResponse;
import com.sharepay.aggregator.modules.account.dto.response.TransactionSummaryResponse;
import com.sharepay.aggregator.modules.account.dto.response.UserBalanceResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/me")
    @Operation(
            summary = "Consulter son profil",
            description = "Retourne le profil complet du marchand connecté : informations personnelles, statut, niveau KYC."
    )
    public ApiResponse<MerchantProfileResponse> getProfile(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(merchantService.getProfile(currentUser.getAccountId()));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "Mettre à jour son profil",
            description = "Modifie le nom complet, le numéro de téléphone, le code pays et/ou l'URL de l'avatar. Seuls les champs fournis sont mis à jour (PATCH sémantique)."
    )
    public ApiResponse<MerchantProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(merchantService.updateProfile(currentUser.getAccountId(), request));
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Changer son mot de passe",
            description = "Vérifie le mot de passe actuel puis le remplace par le nouveau. Non disponible pour les comptes OAuth."
    )
    public void changePassword(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        merchantService.changePassword(currentUser.getAccountId(), request);
    }

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

    @GetMapping("/transactions")
    @Operation(
            summary = "Liste paginée des transactions",
            description = """
                    Retourne les transactions pay-in du marchand connecté, paginées et filtrables.
                    - `page`   : numéro de page (0-based, défaut : 0)
                    - `size`   : nombre d'éléments par page (défaut : 20, max recommandé : 100)
                    - `status` : filtre optionnel — `PENDING` | `SUCCESS` | `FAILED` | `CANCELLED` | `REFUNDED`
                    - `type`   : filtre optionnel — `CHECKOUT` | `CHARGE` | `FUND_COLLECTION`
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

    @GetMapping("/providers")
    @Operation(
            summary = "Providers disponibles pour les retraits",
            description = "Retourne la liste des opérateurs de paiement actifs pour initier un retrait."
    )
    public ApiResponse<List<PaymentProviderResponse>> getWithdrawalProviders() {
        return ApiResponse.success(merchantService.getWithdrawalProviders());
    }

}
