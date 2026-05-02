package com.sharepay.aggregator.modules.account.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@JsonPropertyOrder({"id", "currency", "availableAmount", "pendingAmount", "updatedAt"})
@Schema(description = "Solde du marchand pour une devise donnée")
public class UserBalanceResponse {

    private UUID id;

    @Schema(example = "XAF")
    private String currency;

    @Schema(description = "Montant disponible pour un virement", example = "50000")
    private Long availableAmount;

    @Schema(description = "Montant en cours de traitement (payout initié, non encore confirmé)", example = "10000")
    private Long pendingAmount;

    private OffsetDateTime updatedAt;
}
