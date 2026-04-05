package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requête pour encrypter un nouveau mot de passe via un jeton autorisé")
public class ResetPasswordRequest {

    @NotBlank(message = "Le jeton de réinitialisation est requis")
    @Schema(description = "Jeton (JWT) court obtenu après validation de l'OTP")
    private String resetToken;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*]).{8,}$",
            message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial")
    @Schema(description = "Nouveau mot de passe de l'utilisateur", example = "NewPassword123!")
    private String newPassword;
}
