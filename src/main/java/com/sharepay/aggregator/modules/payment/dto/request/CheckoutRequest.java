package com.sharepay.aggregator.modules.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Création d'une session de paiement web (checkout)")
public class CheckoutRequest {

    @Size(max = 255)
    @Schema(description = "Référence interne du marchand (libre)", example = "CMD-2024-001")
    private String merchantReference;

    @NotNull
    @Min(1)
    @Schema(description = "Montant en unité de base de la devise (ex : centimes)", example = "5000")
    private Long amount;

    @NotBlank
    @Size(max = 3)
    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @Size(max = 255)
    @Schema(description = "Description affichée sur la page de paiement", example = "Paiement commande #001")
    private String description;

    @Size(max = 500)
    @Schema(description = "URL de redirection après paiement réussi", example = "https://mon-site.com/success")
    private String successUrl;

    @Size(max = 500)
    @Schema(description = "URL de redirection après annulation", example = "https://mon-site.com/cancel")
    private String cancelUrl;
}
