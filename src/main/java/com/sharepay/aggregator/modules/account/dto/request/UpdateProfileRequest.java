package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête de mise à jour du profil marchand")
public class UpdateProfileRequest {

    @Size(min = 2, max = 255, message = "Le nom complet doit contenir entre 2 et 255 caractères")
    @Schema(description = "Nom complet", example = "DJINE SINTO PAFING")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Le numéro de téléphone n'est pas valide")
    @Schema(description = "Numéro de téléphone avec indicatif", example = "+237695242663")
    private String phone;

    @Size(min = 2, max = 3, message = "Le code pays doit contenir 2 ou 3 caractères")
    @Schema(description = "Code pays ISO", example = "CM")
    private String country;

    @Schema(description = "URL publique de l'avatar", example = "https://cdn.example.com/avatars/usr-001.jpg")
    private String avatarUrl;
}
