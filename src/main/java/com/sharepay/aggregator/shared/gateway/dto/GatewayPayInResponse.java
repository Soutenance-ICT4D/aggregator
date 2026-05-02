package com.sharepay.aggregator.shared.gateway.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayPayInResponse {

    /** Identifiant de la transaction côté provider */
    private String providerTransactionId;

    /** PROCESSING si la demande est en attente de confirmation du payeur, FAILED si rejet immédiat */
    private String status;

    private String message;
}
