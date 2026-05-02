package com.sharepay.aggregator.modules.webhook.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "Payload personnalisé pour le test webhook")
public class TestWebhookRequest {

    @Schema(
            description = "Contenu libre envoyé dans le champ `data` du payload de test",
            example = "{\"message\": \"Mon test personnalisé\", \"orderId\": \"12345\"}"
    )
    private Map<String, Object> data;
}
