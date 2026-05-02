package com.sharepay.aggregator.shared.gateway.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GatewayPayInRequest {

    /** Référence interne SharePay (PI-XXXX) */
    private String reference;

    /** UUID utilisé comme identifiant côté provider */
    private String providerRef;

    private Long   amount;
    private String currency;

    /** Numéro de téléphone du payeur (ex: 237690000000) */
    private String payerAccount;
    private String payerName;

    private String description;

    /** URL de callback que le provider appellera après confirmation */
    private String callbackUrl;
}
