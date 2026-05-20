package com.sharepay.aggregator.modules.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Ajout d'un compte de retrait")
public class CreateWithdrawalAccountRequest {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "Code du provider (ex : MTN_MOMO_CM)", example = "MTN_MOMO_CM")
    private String providerCode;

    @NotBlank
    @Pattern(regexp = "\\d{8,15}", message = "Le numéro de compte doit contenir entre 8 et 15 chiffres")
    @Schema(description = "Numéro du compte de retrait", example = "237690000000")
    private String accountNumber;

    @NotBlank
    @Size(max = 255)
    @Schema(description = "Nom du titulaire du compte", example = "Jean Dupont")
    private String accountName;

    @Schema(description = "Définir comme compte par défaut", example = "false")
    private boolean isDefault = false;
}
