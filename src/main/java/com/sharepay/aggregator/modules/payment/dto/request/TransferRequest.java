package com.sharepay.aggregator.modules.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Initiation d'un virement vers un bénéficiaire (pay-out)")
public class TransferRequest {

    @Size(max = 255)
    @Schema(description = "Référence interne du marchand", example = "PAYOUT-2024-001")
    private String merchantReference;

    @NotNull
    @Min(1)
    @Schema(description = "Montant à envoyer en unité de base de la devise", example = "10000")
    private Long amount;

    @NotBlank
    @Size(max = 3)
    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @Size(max = 255)
    @Schema(description = "Description du virement", example = "Remboursement client #001")
    private String description;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Code du moyen de paiement (provider)", example = "MTN_MOMO_CM")
    private String paymentMethod;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Numéro de compte ou téléphone du bénéficiaire", example = "237690000000")
    private String beneficiaryAccount;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Nom complet du bénéficiaire", example = "Marie Martin")
    private String beneficiaryName;

    @Size(max = 255)
    @Schema(description = "Email du bénéficiaire", example = "marie@exemple.com")
    private String beneficiaryEmail;
}
