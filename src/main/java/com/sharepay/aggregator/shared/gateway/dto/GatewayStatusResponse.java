package com.sharepay.aggregator.shared.gateway.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayStatusResponse {

    /** Identifiant de la transaction côté provider */
    private String providerTransactionId;

    /**
     * Statut normalisé : "SUCCESS", "FAILED", "PENDING"
     */
    private String status;

    private String message;
}
