package com.sharepay.aggregator.modules.apps.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "Requête de mise à jour partielle d'une application (seuls les champs fournis sont modifiés)")
public class UpdateApplicationRequest {

    @Size(min = 2, max = 255, message = "Le nom doit contenir entre 2 et 255 caractères")
    @Schema(description = "Nouveau nom de l'application", example = "Ma Boutique V2")
    private String name;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    @Schema(description = "Nouvelle description", example = "Plateforme e-commerce premium")
    private String description;

    @URL(message = "L'URL du logo doit être une URL valide")
    @Schema(description = "Nouvelle URL du logo", example = "https://maboutique.cm/logo-v2.png")
    private String logoUrl;

    @URL(message = "L'URL du site doit être une URL valide")
    @Schema(description = "Nouvelle URL du site web", example = "https://maboutique.cm")
    private String websiteUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "La couleur doit être un code hexadécimal valide (ex: #3B82F6)")
    @Schema(description = "Nouvelle couleur principale (hex)", example = "#EF4444")
    private String themeColor;

    @Pattern(regexp = "^[A-Z]{3}$", message = "La devise doit être un code ISO 4217 en majuscules (ex: XAF, EUR, USD)")
    @Schema(description = "Nouvelle devise ISO 4217", example = "EUR")
    private String currency;

    @URL(message = "L'URL de webhook doit être une URL valide")
    @Schema(description = "Nouvelle URL de webhook", example = "https://maboutique.cm/webhooks/v2")
    private String webhookUrl;

    @URL(message = "L'URL de succès doit être une URL valide")
    @Schema(description = "Nouvelle URL de succès", example = "https://maboutique.cm/payment/success")
    private String successUrl;

    @URL(message = "L'URL d'annulation doit être une URL valide")
    @Schema(description = "Nouvelle URL d'annulation", example = "https://maboutique.cm/payment/cancel")
    private String cancelUrl;
}
