package com.sharepay.aggregator.modules.collect.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Informations d'une collecte de fonds")
@JsonPropertyOrder({
        "id", "slug", "title", "description", "status",
        "currency", "isAmountFixed", "amount",
        "coverImageUrl",
        "collectCustomerInfo", "thankYouMessage",
        "expiresAt", "applicationId", "applicationName",
        "createdAt", "updatedAt"
})
public class FundCollectionResponse {

    @Schema(description = "Identifiant unique de la collecte")
    private UUID id;

    @Schema(description = "Slug unique utilisé dans l'URL publique de la collecte", example = "collecte-jeunes-a1b2c3")
    private String slug;

    @Schema(description = "Titre de la collecte")
    private String title;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Description de la collecte")
    private String description;

    @Schema(description = "Statut de la collecte")
    private FundCollectionStatus status;

    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @Schema(description = "Indique si le montant est fixe")
    private boolean isAmountFixed;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Montant fixe en unité de base de la devise. Null si montant libre.", example = "5000")
    private Long amount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "URL de l'image de couverture")
    private String coverImageUrl;

    @Schema(description = "Collecte les informations du contributeur")
    private boolean collectCustomerInfo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Message affiché après une contribution réussie")
    private String thankYouMessage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Date d'expiration de la collecte. Null si pas de limite.")
    private OffsetDateTime expiresAt;

    @Schema(description = "Identifiant de l'application propriétaire")
    private UUID applicationId;

    @Schema(description = "Nom de l'application propriétaire")
    private String applicationName;

    @Schema(description = "Date de création")
    private OffsetDateTime createdAt;

    @Schema(description = "Date de mise à jour")
    private OffsetDateTime updatedAt;
}
