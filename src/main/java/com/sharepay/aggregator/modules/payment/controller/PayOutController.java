package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.PayOutStatusResponse;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Retrait", description = "Retraits - virements vers bénéficiaires")
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Virement initié avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransferResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Requête invalide - `VALIDATION_ERROR` (champ obligatoire manquant ou mal formé), " +
                    "`CURRENCY_MISMATCH` (devise incompatible avec le moyen de paiement), " +
                    "`AMOUNT_BELOW_MINIMUM` / `AMOUNT_ABOVE_MAXIMUM` (montant hors des bornes du provider) ou `MALFORMED_JSON`",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "`PROVIDER_NOT_FOUND` - le moyen de paiement (`paymentMethod`) indiqué est inconnu",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "`INSUFFICIENT_BALANCE` - le solde disponible du marchand est insuffisant pour couvrir le montant et les frais",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut du virement retourné avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PayOutStatusResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "`TRANSACTION_NOT_FOUND` - aucun virement ne correspond à cette `reference` pour votre application",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<PayOutStatusResponse> checkStatus(
            @PathVariable String reference,
            @AuthenticationPrincipal String apiKeyId
    ) {
        PayOutStatusResponse response = payOutService.checkStatus(UUID.fromString(apiKeyId), reference);
        return ApiResponse.success(response);
    }

}
