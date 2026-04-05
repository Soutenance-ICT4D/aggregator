package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requête de vérification de l'email via OTP")
public class VerifyEmailRequest {

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "dsintopafing@gmail.com")
    private String email;

    @NotBlank(message = "Le code OTP est requis")
    @Schema(description = "Code de vérification à 6 chiffres reçu par email", example = "123456")
    private String otp;
}
