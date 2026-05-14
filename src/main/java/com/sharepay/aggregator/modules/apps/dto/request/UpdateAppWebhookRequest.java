package com.sharepay.aggregator.modules.apps.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Requête de mise à jour de l'URL webhook d'une application")
public class UpdateAppWebhookRequest {

    @URL(message = "L'URL de webhook doit être une URL valide")
    @Schema(
            description = "Nouvelle URL de webhook. Passer null ou une chaîne vide pour désactiver les notifications.",
            example = "https://maboutique.cm/webhooks/sharepay"
    )
    private String webhookUrl;
}
