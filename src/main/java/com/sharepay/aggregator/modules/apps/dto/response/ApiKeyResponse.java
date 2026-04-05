package com.sharepay.aggregator.modules.apps.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Informations d'une clé API")
@JsonPropertyOrder({
        "id", "name", "keyPrefix", "environment", "active",
        "plainTextKey",
        "lastUsedAt", "createdAt", "updatedAt"
})
public class ApiKeyResponse {

    @Schema(description = "Identifiant unique de la clé")
    private UUID id;

    @Schema(description = "Nom descriptif de la clé")
    private String name;

    @Schema(description = "Préfixe de la clé (pour identification visuelle)", example = "sk_live_a1b2c3d4")
    private String keyPrefix;

    @Schema(description = "Environnement de la clé")
    private ApiKeyEnvironment environment;

    @Schema(description = "Indique si la clé est active")
    private boolean active;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description = "Valeur brute de la clé. Retournée une seule fois à la création ou à la rotation. À stocker immédiatement.",
            example = "sk_live_a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
    )
    private String plainTextKey;

    @Schema(description = "Date de dernière utilisation")
    private OffsetDateTime lastUsedAt;

    @Schema(description = "Date de création")
    private OffsetDateTime createdAt;

    @Schema(description = "Date de mise à jour")
    private OffsetDateTime updatedAt;
}
