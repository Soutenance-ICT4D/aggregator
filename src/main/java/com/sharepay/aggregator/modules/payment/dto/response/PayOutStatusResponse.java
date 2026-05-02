package com.sharepay.aggregator.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
        "reference", "status",
        "amount", "currency", "description",
        "paymentMethod", "beneficiaryAccount", "beneficiaryName", "beneficiaryEmail",
        "failureCode", "failureReason"
})
@Schema(description = "Statut d'un payout")
public class PayOutStatusResponse {

    @Schema(description = "Référence unique du payout", example = "PO-A1B2C3D4E5F6")
    private String reference;

    @Schema(description = "Statut courant du payout")
    private TransactionStatus status;

    @Schema(description = "Montant en unité de base de la devise", example = "10000")
    private Long amount;

    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Description du virement")
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Moyen de paiement utilisé")
    private String paymentMethod;

    @Schema(description = "Numéro de compte ou téléphone du bénéficiaire")
    private String beneficiaryAccount;

    @Schema(description = "Nom du bénéficiaire")
    private String beneficiaryName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Email du bénéficiaire")
    private String beneficiaryEmail;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Code d'erreur (disponible si statut FAILED)")
    private String failureCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Message d'erreur détaillé (disponible si statut FAILED)")
    private String failureReason;
}
