package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.payment.dto.request.ChargeRequest;
import com.sharepay.aggregator.modules.payment.dto.request.CheckoutRequest;
import com.sharepay.aggregator.modules.payment.dto.response.ChargeResponse;
import com.sharepay.aggregator.modules.payment.dto.response.CheckoutResponse;
import com.sharepay.aggregator.modules.payment.dto.response.PayInStatusResponse;
import com.sharepay.aggregator.modules.payment.service.PayInService;
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
@RequestMapping("/api/v1/pay-in")
@Tag(name = "Paiement", description = "Paiements entrants — checkout web et charge directe")
@SecurityRequirement(name = "apiKeyAuth")
public class PayInController {

    private final PayInService payInService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Créer une session de paiement web (checkout)",
            description = "Génère une session de paiement valable 30 minutes. " +
                    "Redirigez votre client vers le `paymentUrl` retourné pour qu'il finalise le paiement."
    )
    public ApiResponse<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal String apiKeyId
    ) {
        CheckoutResponse response = payInService.createCheckout(UUID.fromString(apiKeyId), request);
        return ApiResponse.success("Session de paiement créée.", response);
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Paiement direct (charge)",
            description = "Initie un paiement immédiat sans page de paiement web. " +
                    "Le provider et le compte payeur doivent être fournis dans la requête. " +
                    "Supporte une clé d'idempotence pour éviter les doublons."
    )
    public ApiResponse<ChargeResponse> charge(
            @Valid @RequestBody ChargeRequest request,
            @AuthenticationPrincipal String apiKeyId
    ) {
        ChargeResponse response = payInService.createCharge(UUID.fromString(apiKeyId), request);
        return ApiResponse.success("Paiement initié.", response);
    }

    @GetMapping("/check_status/{reference}")
    @Operation(
            summary = "Vérifier le statut d'un paiement",
            description = "Retourne le statut courant d'une transaction entrante identifiée par sa `reference`. " +
                    "Fonctionne aussi bien pour les transactions CHECKOUT que CHARGE."
    )
    public ApiResponse<PayInStatusResponse> checkStatus(
            @PathVariable String reference,
            @AuthenticationPrincipal String apiKeyId
    ) {
        PayInStatusResponse response = payInService.checkStatus(UUID.fromString(apiKeyId), reference);
        return ApiResponse.success(response);
    }

}
