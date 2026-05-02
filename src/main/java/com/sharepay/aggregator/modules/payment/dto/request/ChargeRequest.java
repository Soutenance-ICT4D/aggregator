package com.sharepay.aggregator.modules.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Paiement direct (charge) sans page de paiement web")
public class ChargeRequest {

    @Size(max = 255)
    @Schema(description = "Référence interne du marchand", example = "CMD-2024-001")
    private String merchantReference;

    @NotNull
    @Min(1)
    @Schema(description = "Montant en unité de base de la devise", example = "5000")
    private Long amount;

    @NotBlank
    @Size(max = 3)
    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @Size(max = 255)
    @Schema(description = "Description de la transaction", example = "Paiement commande #001")
    private String description;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Code du moyen de paiement (provider)", example = "MTN_MOMO_CM")
    private String paymentMethod;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Numéro de compte ou téléphone du payeur", example = "237690000000")
    private String payerAccount;

    @Size(max = 255)
    @Schema(description = "Nom du payeur", example = "Jean Dupont")
    private String payerName;

    @Size(max = 255)
    @Schema(description = "Email du payeur", example = "jean@exemple.com")
    private String payerEmail;

    @Size(max = 100)
    @Schema(description = "Clé d'idempotence pour éviter les doublons en cas de retry", example = "idem-cmd-001-v1")
    private String idempotencyKey;
}
