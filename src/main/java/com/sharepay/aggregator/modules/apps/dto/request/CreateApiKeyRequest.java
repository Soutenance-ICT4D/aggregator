package com.sharepay.aggregator.modules.apps.dto.request;

import com.sharepay.aggregator.shared.constant.ApiKeyEnvironment;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête de création d'une clé API")
public class CreateApiKeyRequest {

    @NotBlank(message = "Le nom de la clé est requis")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Schema(description = "Nom descriptif de la clé", example = "Clé production principale")
    private String name;

    @NotNull(message = "L'environnement est requis")
    @Schema(description = "Environnement de la clé (LIVE ou TEST)", example = "LIVE")
    private ApiKeyEnvironment environment;
}
