package com.sharepay.aggregator.shared.gateway.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayPayOutResponse {

    /** Identifiant de la transaction côté provider */
    private String providerTransactionId;

    /** PROCESSING si la demande est acceptée, FAILED si rejet immédiat */
    private String status;

    private String message;
}
