package com.sharepay.aggregator.modules.apps.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import com.sharepay.aggregator.shared.constant.AppStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Informations d'une application")
@JsonPropertyOrder({
        "id", "name", "description", "status", "currency",
        "themeColor", "logoUrl", "websiteUrl",
        "webhookUrl", "plainTextWebhookSecret", "successUrl", "cancelUrl",
        "activeKeyPrefix", "activeKeyEnvironment",
        "createdAt", "updatedAt"
})
public class ApplicationResponse {

    @Schema(description = "Identifiant unique de l'application")
    private UUID id;

    @Schema(description = "Nom de l'application")
    private String name;

    @Schema(description = "Description de l'application")
    private String description;

    @Schema(description = "Statut de l'application")
    private AppStatus status;

    @Schema(description = "Devise ISO 4217")
    private String currency;

    @Schema(description = "Couleur principale (hex)")
    private String themeColor;

    @Schema(description = "URL du logo")
    private String logoUrl;

    @Schema(description = "URL du site web")
    private String websiteUrl;

    @Schema(description = "URL de webhook")
    private String webhookUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description = "Secret webhook en clair. Retourné **une seule fois** à la création ou à la rotation. " +
                    "À stocker immédiatement — utilisez-le pour vérifier la signature HMAC-SHA256 des webhooks entrants.",
            example = "whsec_a1b2c3d4e5f6..."
    )
    private String plainTextWebhookSecret;

    @Schema(description = "URL de succès")
    private String successUrl;

    @Schema(description = "URL d'annulation")
    private String cancelUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Préfixe de la clé API active (ex: sk_live_a1b2c3d4). Absent si aucune clé active.", example = "sk_live_a1b2c3d4")
    private String activeKeyPrefix;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Environnement de la clé API active. Absent si aucune clé active.")
    private ApiKeyEnvironment activeKeyEnvironment;

    @Schema(description = "Date de création")
    private OffsetDateTime createdAt;

    @Schema(description = "Date de mise à jour")
    private OffsetDateTime updatedAt;
}
