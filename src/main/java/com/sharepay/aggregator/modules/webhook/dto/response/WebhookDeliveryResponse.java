package com.sharepay.aggregator.modules.webhook.dto.response;

import com.sharepay.aggregator.modules.webhook.constant.WebhookDeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Getter
public class WebhookDeliveryResponse {

    private UUID id;
    private String eventName;
    private WebhookDeliveryStatus status;
    private Integer httpStatus;
    private int attemptCount;
    private OffsetDateTime nextRetryAt;
    private String lastError;
    private OffsetDateTime createdAt;
}
