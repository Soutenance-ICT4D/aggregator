package com.sharepay.aggregator.shared.web;

import com.sharepay.aggregator.shared.dto.HealthCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/v1/public/health")
@Tag(name = "Health Check", description = "Endpoints pour vérifier l'état du système")
@SecurityRequirements() /* Public endpoints, no lock icon */
public class HealthController {

    @Operation(summary = "Vérifier l'état de l'API", description = "Retourne un message de succès si l'API est fonctionnelle. Accessible publiquement.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "L'API est en ligne",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = HealthCheckResponse.class)))
    })
    @GetMapping
    public HealthCheckResponse checkHealth() {
        return HealthCheckResponse.builder()
                .success(true)
                .message("Sharepay Aggregator est opérationnel")
                .build();
    }
}