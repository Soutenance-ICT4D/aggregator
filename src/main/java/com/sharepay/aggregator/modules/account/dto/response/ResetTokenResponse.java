package com.sharepay.aggregator.modules.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse contenant le jeton pour procéder à la modification du mot de passe")
public class ResetTokenResponse {
    
    @Schema(description = "Jeton JWT courte-durée (5 min) à fournir à l'étape finale")
    private String resetToken;
}
