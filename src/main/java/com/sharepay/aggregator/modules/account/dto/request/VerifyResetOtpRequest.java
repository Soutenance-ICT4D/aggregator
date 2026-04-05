package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requête pour vérifier l'OTP de réinitialisation de mot de passe")
public class VerifyResetOtpRequest {

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "dsintopafing@gmail.com")
    private String email;

    @NotBlank(message = "Le code OTP est requis")
    @Pattern(regexp = "^\\d{6}$", message = "Le code OTP doit contenir exactement 6 chiffres")
    @Schema(description = "Code OTP reçu par email", example = "123456")
    private String otp;
}
