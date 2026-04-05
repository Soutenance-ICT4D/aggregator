package com.sharepay.aggregator.modules.apps.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête de création d'une application")
public class CreateApplicationRequest {

    @NotBlank(message = "Le nom de l'application est requis")
    @Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
    @Schema(description = "Nom unique de l'application pour ce compte", example = "Ma Boutique")
    private String name;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    @Schema(description = "Description de l'application", example = "Plateforme e-commerce de vente de vêtements")
    private String description;

    @URL(message = "L'URL du logo doit être une URL valide")
    @Schema(description = "URL publique du logo de l'application", example = "https://maboutique.cm/logo.png")
    private String logoUrl;

    @URL(message = "L'URL du site doit être une URL valide")
    @Schema(description = "URL du site web", example = "https://maboutique.cm")
    private String websiteUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit être un code hexadécimal valide (ex: #3B82F6)")
    @Schema(description = "Couleur principale de l'application (hex)", example = "#3B82F6")
    private String themeColor;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO 4217 en majuscules (ex: XAF, EUR, USD)")
    @Schema(description = "Code devise ISO 4217", example = "XAF")
    private String currency;

    @URL(message = "L'URL de webhook doit être une URL valide")
    @Size(max = 500, message = "L'URL de webhook ne peut pas dépasser 500 caractères")
    @Schema(description = "URL de webhook pour les notifications d'événements", example = "https://maboutique.cm/webhooks/sharepay")
    private String webhookUrl;

    @Size(max = 500, message = "L'URL de succès ne peut pas dépasser 500 caractères")
    @Schema(description = "URL de redirection après paiement réussi", example = "https://maboutique.cm/payment/success")
    private String successUrl;

    @Size(max = 500, message = "L'URL d'annulation ne peut pas dépasser 500 caractères")
    @Schema(description = "URL de redirection après paiement annulé", example = "https://maboutique.cm/payment/cancel")
    private String cancelUrl;
}
