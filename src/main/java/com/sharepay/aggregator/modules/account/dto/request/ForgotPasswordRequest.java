package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Requête pour initier la réinitialisation du mot de passe")
public class ForgotPasswordRequest {
    
    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email du compte à récupérer", example = "dsintopafing@gmail.com")
    private String email;
}
