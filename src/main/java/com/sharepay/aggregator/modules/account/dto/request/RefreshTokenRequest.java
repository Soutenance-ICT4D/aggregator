package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requête de manipulation du Refresh Token (Renouvellement ou Déconnexion)")
public class RefreshTokenRequest {

    @NotBlank(message = "Le refresh token est requis")
    @Schema(description = "Le refresh token délivré lors de la connexion", example = "a1b2c3d4-e5f6-7a8b-9c0d-e1f2a3b4c5d6")
    private String refreshToken;
}
