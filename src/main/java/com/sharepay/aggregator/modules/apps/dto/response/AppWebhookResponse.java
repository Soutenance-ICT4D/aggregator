package com.sharepay.aggregator.modules.apps.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Configuration webhook d'une application")
@JsonPropertyOrder({
        "applicationId", "applicationName",
        "webhookUrl", "webhookSecretPrefix", "plainTextWebhookSecret",
        "updatedAt"
})
public class AppWebhookResponse {

    @Schema(description = "Identifiant de l'application")
    private UUID applicationId;

    @Schema(description = "Nom de l'application")
    private String applicationName;

    @Schema(description = "URL de réception des événements webhook", example = "https://maboutique.cm/webhooks/sharepay")
    private String webhookUrl;

    @Schema(
            description = "Préfixe masqué du secret webhook actuel, pour identification visuelle.",
            example = "whsec_a1b2c3d4e5f6..."
    )
    private String webhookSecretPrefix;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description = "Secret webhook en clair. Retourné **une seule fois** après une rotation. " +
                    "À stocker immédiatement — utilisez-le pour vérifier la signature HMAC-SHA256 des webhooks entrants.",
            example = "whsec_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"
    )
    private String plainTextWebhookSecret;

    @Schema(description = "Date de dernière modification de la configuration webhook")
    private OffsetDateTime updatedAt;
}
