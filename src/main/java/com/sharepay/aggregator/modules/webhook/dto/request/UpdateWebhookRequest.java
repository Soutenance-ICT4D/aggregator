package com.sharepay.aggregator.modules.webhook.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Mise à jour de la configuration webhook d'une application")
public class UpdateWebhookRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "URL qui recevra les notifications webhook", example = "https://mon-site.com/webhooks/sharepay")
    private String webhookUrl;
}
