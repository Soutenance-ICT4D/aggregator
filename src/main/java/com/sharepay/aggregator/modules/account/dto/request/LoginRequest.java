package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requête de connexion")
public class LoginRequest {

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email de l'utilisateur", example = "dsintopafing@gmail.com")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Schema(description = "Mot de passe de l'utilisateur", example = "Password123!")
    private String password;
}
