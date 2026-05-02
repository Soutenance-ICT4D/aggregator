package com.sharepay.aggregator.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"reference", "status", "amount", "currency", "description", "paymentUrl"})
@Schema(description = "Réponse à la création d'une session de paiement web")
public class CheckoutResponse {

    @Schema(description = "Référence unique de la transaction", example = "PI-A1B2C3D4E5F6")
    private String reference;

    @Schema(description = "Statut initial de la session", example = "PENDING")
    private TransactionStatus status;

    @Schema(description = "Montant en unité de base de la devise", example = "5000")
    private Long amount;

    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Description de la transaction")
    private String description;

    @Schema(
            description = "URL vers laquelle rediriger le client pour compléter le paiement. " +
                    "La session expire après 30 minutes.",
            example = "https://checkout.sharepay.com/pay/cs_a1b2c3..."
    )
    private String paymentUrl;
}
