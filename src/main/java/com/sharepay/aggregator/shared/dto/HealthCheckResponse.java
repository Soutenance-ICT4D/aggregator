package com.sharepay.aggregator.shared.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"success", "message"})
public class HealthCheckResponse {
    @Schema(example = "true")
    private boolean success;

    @Schema(example = "Sharepay Aggregator est opérationnel")
    private String message;
}
