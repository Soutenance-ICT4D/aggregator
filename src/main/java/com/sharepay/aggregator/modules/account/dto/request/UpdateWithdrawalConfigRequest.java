package com.sharepay.aggregator.modules.account.dto.request;

import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.constant.WithdrawalPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Mise à jour de la configuration de retrait automatique")
public class UpdateWithdrawalConfigRequest {

    @NotNull
    @Schema(description = "Mode de retrait", example = "MANUAL")
    private WithdrawalMode mode;

    @Schema(description = "ID du compte de retrait cible (requis pour INSTANT, THRESHOLD, PERIODIC)")
    private UUID accountId;

    @Min(1)
    @Schema(description = "Seuil de déclenchement en unité de base (requis pour THRESHOLD)", example = "50000")
    private Long thresholdAmount;

    @Schema(description = "Périodicité (requis pour PERIODIC)", example = "MONTHLY")
    private WithdrawalPeriod period;
}
