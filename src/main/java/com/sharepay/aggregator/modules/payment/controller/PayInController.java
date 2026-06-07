package com.sharepay.aggregator.modules.payment.controller;

import com.sharepay.aggregator.modules.payment.dto.request.ChargeRequest;
import com.sharepay.aggregator.modules.payment.dto.request.CheckoutRequest;
import com.sharepay.aggregator.modules.payment.dto.response.ChargeResponse;
import com.sharepay.aggregator.modules.payment.dto.response.CheckoutResponse;
import com.sharepay.aggregator.modules.payment.dto.response.PayInStatusResponse;
import com.sharepay.aggregator.modules.payment.service.PayInService;
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
@RequestMapping("/api/v1/pay-in")
@Tag(name = "Paiement", description = "Paiements entrants - checkout web et charge directe")
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session de paiement créée avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckoutResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Corps de requête invalide - `VALIDATION_ERROR` (champ obligatoire manquant ou mal formé) ou `MALFORMED_JSON`",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Paiement initié avec succès (ou charge idempotente déjà existante retournée telle quelle)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChargeResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Requête invalide - `VALIDATION_ERROR` (champ obligatoire manquant ou mal formé), " +
                    "`CURRENCY_MISMATCH` (devise incompatible avec le moyen de paiement), " +
                    "`AMOUNT_BELOW_MINIMUM` / `AMOUNT_ABOVE_MAXIMUM` (montant hors des bornes du provider) ou `MALFORMED_JSON`",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "`PROVIDER_NOT_FOUND` - le moyen de paiement (`paymentMethod`) indiqué est inconnu",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statut de la transaction retourné avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PayInStatusResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentification par clé API échouée - `UNAUTHORIZED` (clé manquante/invalide) ou `API_KEY_INVALID` (clé authentifiée mais introuvable en base)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "`TRANSACTION_NOT_FOUND` - aucune transaction entrante ne correspond à cette `reference` pour votre application",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<PayInStatusResponse> checkStatus(
            @PathVariable String reference,
            @AuthenticationPrincipal String apiKeyId
    ) {
        PayInStatusResponse response = payInService.checkStatus(UUID.fromString(apiKeyId), reference);
        return ApiResponse.success(response);
    }

}
