package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.PayOutStatusResponse;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pay-out")
@Tag(name = "Retrait", description = "Retraits — virements vers bénéficiaires")
@SecurityRequirement(name = "apiKeyAuth")
public class PayOutController {

    private final PayOutService payOutService;

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Initier un virement (payout)",
            description = "Crée un virement vers un bénéficiaire. " +
                    "Le montant total (amount + fees) est débité du solde disponible du marchand. " +
                    "La transaction passe immédiatement en statut PROCESSING."
    )
    public ApiResponse<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal String apiKeyId
    ) {
        TransferResponse response = payOutService.createTransfer(UUID.fromString(apiKeyId), request);
        return ApiResponse.success("Virement initié.", response);
    }

    @GetMapping("/check_status/{reference}")
    @Operation(
            summary = "Vérifier le statut d'un payout",
            description = "Retourne le statut courant d'une transaction sortante identifiée par sa `reference`."
    )
    public ApiResponse<PayOutStatusResponse> checkStatus(
            @PathVariable String reference,
            @AuthenticationPrincipal String apiKeyId
    ) {
        PayOutStatusResponse response = payOutService.checkStatus(UUID.fromString(apiKeyId), reference);
        return ApiResponse.success(response);
    }

}
