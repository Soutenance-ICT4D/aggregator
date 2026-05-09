package com.sharepay.aggregator.modules.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Provider de paiement disponible pour les retraits")
public class PaymentProviderResponse {

    @Schema(description = "Code technique du provider", example = "MTN_MOMO_CM")
    private String code;

    @Schema(description = "Nom lisible du provider", example = "MTN Mobile Money")
    private String name;

    @Schema(description = "Type de provider", example = "MOBILE_MONEY")
    private String type;

    @Schema(description = "Devise supportée", example = "XAF")
    private String currency;

    @Schema(description = "Montant minimum", example = "500")
    private Long minAmount;

    @Schema(description = "Montant maximum", example = "5000000")
    private Long maxAmount;
}
