package com.sharepay.aggregator.modules.collect.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Requête de création d'une collecte de fonds")
public class CreateFundCollectionRequest {

    @NotBlank(message = "Le titre de la collecte est requis")
    @Size(min = 2, max = 255, message = "Le titre doit contenir entre 2 et 255 caractères")
    @Schema(description = "Titre de la collecte", example = "Collecte pour l'association des jeunes")
    private String title;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    @Schema(description = "Description détaillée de la collecte")
    private String description;

    @URL(message = "L'URL de l'image de couverture doit être une URL valide")
    @Schema(description = "URL de l'image de couverture", example = "https://maboutique.cm/cover.png")
    private String coverImageUrl;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO 4217 en majuscules (ex: XAF, EUR, USD)")
    @Schema(description = "Devise ISO 4217", example = "XAF")
    private String currency;

    @Schema(description = "Indique si le montant est fixe (true) ou libre (false)", example = "false")
    private Boolean isAmountFixed;

    @Positive(message = "Le montant doit être supérieur à 0")
    @Schema(description = "Montant fixe en unité de base de la devise (ex: centimes). Obligatoire si isAmountFixed = true.", example = "5000")
    private Long amount;

    @Future(message = "La date d'expiration doit être dans le futur")
    @Schema(description = "Date et heure d'expiration de la collecte (optionnel)")
    private OffsetDateTime expiresAt;

    @Schema(description = "Collecte les informations du contributeur (nom, email…)", example = "true")
    private Boolean collectCustomerInfo;

    @Size(max = 500, message = "Le message de remerciement ne peut pas dépasser 500 caractères")
    @Schema(description = "Message affiché après une contribution réussie", example = "Merci pour votre générosité !")
    private String thankYouMessage;
}
