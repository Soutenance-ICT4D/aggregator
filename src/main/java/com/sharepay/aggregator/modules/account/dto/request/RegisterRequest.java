package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête d'inscription d'un nouvel utilisateur")
public class RegisterRequest {

    @NotBlank(message = "Le nom complet est requis")
    @Schema(description = "Nom complet de l'utilisateur", example = "DJINE SINTO PAFING")
    private String fullName;

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email professionnelle ou personnelle", example = "dsintopafing@gmail.com")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*]).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial")
    @Schema(description = "Mot de passe sécurisé", example = "Password123!")
    private String password;

    @NotBlank(message = "Le numéro de téléphone est requis")
    @Schema(description = "Numéro de téléphone avec indicatif", example = "+237695242663")
    private String phone;

    @NotBlank(message = "Le pays est requis")
    @Size(max = 3, message = "Le code pays ne peut pas dépasser 3 caractères")
    @Schema(description = "Code pays ISO", example = "CM")
    private String country;
}

