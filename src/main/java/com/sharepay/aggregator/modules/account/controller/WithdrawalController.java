package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.request.CreateWithdrawalAccountRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateWithdrawalConfigRequest;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalAccountResponse;
import com.sharepay.aggregator.modules.account.dto.response.WithdrawalConfigResponse;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.modules.account.service.WithdrawalConfigService;
import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.shared.dto.ApiResponse;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/merchants/withdrawals")
@Tag(name = "Retraits Marchand", description = "Gestion des comptes et de la configuration de retrait")
@SecurityRequirement(name = "bearerAuth")
public class WithdrawalController {

    private final WithdrawalConfigService withdrawalConfigService;
    private final MerchantService merchantService;

    // ── Comptes de retrait ────────────────────────────────────────────────────

    @GetMapping("/accounts")
    @Operation(summary = "Lister les comptes de retrait",
               description = "Retourne tous les comptes de retrait enregistrés par le marchand.")
    public ApiResponse<List<WithdrawalAccountResponse>> getAccounts(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(withdrawalConfigService.getAccounts(currentUser.getAccountId()));
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un compte de retrait",
               description = "Enregistre un nouveau compte de retrait.")
    public ApiResponse<WithdrawalAccountResponse> addAccount(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @Valid @RequestBody CreateWithdrawalAccountRequest request
    ) {
        return ApiResponse.success(withdrawalConfigService.addAccount(currentUser.getAccountId(), request));
    }

    @DeleteMapping("/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un compte de retrait")
    public void deleteAccount(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @PathVariable UUID accountId
    ) {
        withdrawalConfigService.deleteAccount(currentUser.getAccountId(), accountId);
    }

    @PatchMapping("/accounts/{accountId}/default")
    @Operation(summary = "Définir un compte comme par défaut")
    public ApiResponse<WithdrawalAccountResponse> setDefaultAccount(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @PathVariable UUID accountId
    ) {
        return ApiResponse.success(withdrawalConfigService.setDefaultAccount(currentUser.getAccountId(), accountId));
    }

    // ── Configuration du mode de retrait ──────────────────────────────────────

    @GetMapping("/config")
    @Operation(summary = "Lire la configuration de retrait",
               description = "Retourne le mode actif et les paramètres associés (seuil, période, compte cible).")
    public ApiResponse<WithdrawalConfigResponse> getConfig(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        return ApiResponse.success(withdrawalConfigService.getConfig(currentUser.getAccountId()));
    }

    @PutMapping("/config")
    @Operation(summary = "Mettre à jour la configuration de retrait",
               description = "Définit le mode (MANUAL, INSTANT, THRESHOLD, PERIODIC) et ses paramètres.")
    public ApiResponse<WithdrawalConfigResponse> updateConfig(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @Valid @RequestBody UpdateWithdrawalConfigRequest request
    ) {
        return ApiResponse.success(withdrawalConfigService.updateConfig(currentUser.getAccountId(), request));
    }

    // ── Retrait manuel ────────────────────────────────────────────────────────

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Initier un retrait manuel",
            description = "Crée un retrait (payout) depuis le solde disponible du marchand vers un compte enregistré."
    )
    public ApiResponse<TransferResponse> withdraw(
            @AuthenticationPrincipal AuthentificatedUser currentUser,
            @Valid @RequestBody TransferRequest request
    ) {
        return ApiResponse.success(merchantService.initiateWithdrawal(currentUser.getAccountId(), request));
    }
}
