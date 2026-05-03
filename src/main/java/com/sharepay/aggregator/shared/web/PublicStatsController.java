package com.sharepay.aggregator.shared.web;

import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.dto.PublicStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stats")
@Tag(name = "Public Stats", description = "Statistiques publiques de la plateforme")
@SecurityRequirements()
@RequiredArgsConstructor
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    @Operation(
            summary = "Statistiques globales de la plateforme",
            description = "Retourne le nombre de marchands actifs, transactions traitées et méthodes de paiement supportées. Accessible publiquement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statistiques retournées avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PublicStatsResponse.class))
            )
    })
    @GetMapping
    public ApiResponse<PublicStatsResponse> getStats() {
        return ApiResponse.success(publicStatsService.getStats());
    }
}
