package com.sharepay.aggregator.modules.collect.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Requête de mise à jour partielle d'une collecte (seuls les champs fournis sont modifiés)")
public class UpdateFundCollectionRequest {

    @Size(min = 2, max = 255, message = "Le titre doit contenir entre 2 et 255 caractères")
    @Schema(description = "Nouveau titre de la collecte", example = "Collecte mise à jour")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    @Schema(description = "Nouvelle description")
    private String description;

    @URL(message = "L'URL de l'image de couverture doit être une URL valide")
    @Schema(description = "Nouvelle URL de l'image de couverture")
    private String coverImageUrl;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO 4217 en majuscules (ex: XAF, EUR, USD)")
    @Schema(description = "Nouvelle devise ISO 4217", example = "EUR")
    private String currency;

    @Schema(description = "Modifie le mode montant fixe / libre")
    private Boolean isAmountFixed;

    @Positive(message = "Le montant doit être supérieur à 0")
    @Schema(description = "Nouveau montant fixe en unité de base de la devise", example = "10000")
    private Long amount;

    @Future(message = "La date d'expiration doit être dans le futur")
    @Schema(description = "Nouvelle date d'expiration. Ignoré si removeExpiresAt est true.")
    private OffsetDateTime expiresAt;

    @Schema(description = "Si true, supprime la date d'expiration (la collecte devient sans limite de temps)", example = "false")
    private Boolean removeExpiresAt;

    @Schema(description = "Active ou désactive la collecte des informations du contributeur")
    private Boolean collectCustomerInfo;

    @Size(max = 500, message = "Le message de remerciement ne peut pas dépasser 500 caractères")
    @Schema(description = "Nouveau message de remerciement")
    private String thankYouMessage;
}
