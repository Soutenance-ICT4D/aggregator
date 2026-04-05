package com.sharepay.aggregator.modules.account.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Réponse de connexion avec les tokens d'accès")
public class AuthLoginResponse {

    @Schema(description = "Token d'accès JWT")
    private String accessToken;

    @Schema(description = "Token de rafraichissement (durée de vie plus longue)")
    private String refreshToken;

    @Schema(description = "Informations sur l'utilisateur connecté")
    private AuthInfoResponse user;
}
