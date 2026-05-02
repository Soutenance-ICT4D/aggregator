package com.sharepay.aggregator.modules.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
        "reference", "type", "status",
        "amount", "currency", "description",
        "paymentMethod", "payerAccount", "payerName", "payerEmail",
        "paymentUrl",
        "failureCode", "failureReason"
})
@Schema(description = "Statut d'une transaction entrante (checkout ou charge)")
public class PayInStatusResponse {

    @Schema(description = "Référence unique de la transaction", example = "PI-A1B2C3D4E5F6")
    private String reference;

    @Schema(description = "Type de transaction")
    private TransactionInType type;

    @Schema(description = "Statut courant de la transaction")
    private TransactionStatus status;

    @Schema(description = "Montant en unité de base de la devise", example = "5000")
    private Long amount;

    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Description de la transaction")
    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Moyen de paiement utilisé (disponible après sélection du provider)")
    private String paymentMethod;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Compte payeur (disponible après initiation du paiement)")
    private String payerAccount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Nom du payeur")
    private String payerName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Email du payeur")
    private String payerEmail;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "URL de paiement (uniquement pour type CHECKOUT en statut PENDING)")
    private String paymentUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Code d'erreur (disponible si statut FAILED)")
    private String failureCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Message d'erreur détaillé (disponible si statut FAILED)")
    private String failureReason;
}
