package com.sharepay.aggregator.modules.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload envoyé par NOKASH lors d'un changement de statut (callback_url)")
public class ProviderWebhookPayload {

    @Schema(description = "Identifiant de la transaction côté NOKASH (providerTransactionId)", example = "65f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "Nouveau statut de la transaction", example = "SUCCESS")
    private String status;

    @Schema(description = "Montant de la transaction")
    private Long amount;

    @Schema(description = "Numéro de téléphone du payeur")
    private String phone;

    @Schema(description = "Référence interne SharePay (TransactionIn.reference)", example = "PI-A1B2C3D4E5F6")
    private String orderId;
}
