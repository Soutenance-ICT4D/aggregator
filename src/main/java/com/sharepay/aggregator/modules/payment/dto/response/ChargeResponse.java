package com.sharepay.aggregator.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"reference", "status", "amount", "currency", "description",
        "paymentMethod", "payerAccount", "payerName", "payerEmail"})
@Schema(description = "Réponse à l'initiation d'un paiement direct (charge)")
public class ChargeResponse {

    @Schema(description = "Référence unique de la transaction", example = "PI-A1B2C3D4E5F6")
    private String reference;

    @Schema(description = "Statut initial du paiement", example = "PROCESSING")
    private TransactionStatus status;

    @Schema(description = "Montant en unité de base de la devise", example = "5000")
    private Long amount;

    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Description de la transaction")
    private String description;

    @Schema(description = "Moyen de paiement utilisé", example = "MTN_MOMO_CM")
    private String paymentMethod;

    @Schema(description = "Numéro de compte ou téléphone du payeur", example = "237690000000")
    private String payerAccount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Nom du payeur")
    private String payerName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Email du payeur")
    private String payerEmail;
}
