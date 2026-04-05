package com.sharepay.aggregator.modules.apps.dto.request;

import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête de rotation de clé API. Révoque toutes les clés actives de l'application et génère une nouvelle clé.")
public class RotateApiKeyRequest {

    @NotBlank(message = "Le nom de la nouvelle clé est requis")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Schema(description = "Nom descriptif de la nouvelle clé", example = "Clé rotation 2025")
    private String name;

    @NotNull(message = "L'environnement de la nouvelle clé est requis")
    @Schema(description = "Environnement de la nouvelle clé (LIVE ou TEST)", example = "LIVE")
    private ApiKeyEnvironment environment;
}
