package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Requête de changement de mot de passe")
public class ChangePasswordRequest {

    @NotBlank(message = "Le mot de passe actuel est requis")
    @Schema(description = "Mot de passe actuel", example = "OldPassword123!")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!*]).{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial"
    )
    @Schema(description = "Nouveau mot de passe sécurisé", example = "NewPassword123!")
    private String newPassword;
}
