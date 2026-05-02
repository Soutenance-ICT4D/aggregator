package com.sharepay.aggregator.modules.webhook.dto.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@JsonPropertyOrder({"delivered", "webhookUrl", "httpStatus", "sentAt"})
@Schema(description = "Résultat du test webhook")
public class TestWebhookResponse {

    @Schema(description = "Indique si l'URL a répondu avec un statut 2xx", example = "true")
    private boolean delivered;

    @Schema(description = "URL testée", example = "https://example.com/webhook")
    private String webhookUrl;

    @Schema(description = "Code HTTP retourné par l'URL (null si l'URL est inaccessible)", example = "200")
    private Integer httpStatus;

    @Schema(description = "Horodatage de l'envoi")
    private OffsetDateTime sentAt;
}
