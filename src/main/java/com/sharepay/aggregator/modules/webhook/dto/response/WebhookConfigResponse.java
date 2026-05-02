package com.sharepay.aggregator.modules.webhook.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@JsonPropertyOrder({"applicationId", "applicationName", "webhookUrl", "webhookSecretPrefix", "updatedAt"})
@Schema(description = "Configuration webhook d'une application")
public class WebhookConfigResponse {

    @Schema(description = "Identifiant de l'application")
    private UUID applicationId;

    @Schema(description = "Nom de l'application")
    private String applicationName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "URL configurée pour la réception des webhooks", example = "https://mon-site.com/webhooks/sharepay")
    private String webhookUrl;

    @Schema(
            description = "Préfixe masqué du secret webhook (whsec_ + 8 premiers caractères). " +
                    "Pour obtenir le secret complet, utilisez la rotation via PATCH /api/v1/apps/{appId}/webhook-secret/rotate.",
            example = "whsec_a1b2c3d4..."
    )
    private String webhookSecretPrefix;

    @Schema(description = "Date de dernière mise à jour de l'application")
    private OffsetDateTime updatedAt;
}
