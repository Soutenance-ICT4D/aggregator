package com.sharepay.aggregator.modules.apps.controller;

import com.sharepay.aggregator.modules.apps.dto.request.CreateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.request.UpdateApplicationRequest;
import com.sharepay.aggregator.modules.apps.dto.response.ApplicationResponse;
import com.sharepay.aggregator.modules.apps.service.ApplicationService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Gestion des applications marchandes")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    // 1. Créer une application
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Créer une application",
            description = "Crée une nouvelle application pour l'utilisateur authentifié. Un webhook secret est généré automatiquement."
    )
    public ApiResponse<ApplicationResponse> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApplicationResponse response = applicationService.createApplication(currentUser.getAccountId(), request);
        return ApiResponse.success("Application créée avec succès.", response);
    }

    // 2. Lister toutes les applications (hors supprimées)
    @GetMapping
    @Operation(
            summary = "Lister les applications",
            description = "Retourne toutes les applications de l'utilisateur authentifié, à l'exception de celles supprimées."
    )
    public ApiResponse<List<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        List<ApplicationResponse> apps = applicationService.getApplications(currentUser.getAccountId());
        return ApiResponse.success(apps);
    }

    // 3. Obtenir une application en particulier
    @GetMapping("/{appId}")
    @Operation(
            summary = "Détail d'une application",
            description = "Retourne les informations d'une application appartenant à l'utilisateur authentifié."
    )
    public ApiResponse<ApplicationResponse> getApplication(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApplicationResponse app = applicationService.getApplication(currentUser.getAccountId(), appId);
        return ApiResponse.success(app);
    }
    // 4. Mettre à jour partiellement une application
    @PatchMapping("/{appId}")
    @Operation(
            summary = "Mettre à jour une application",
            description = "Mise à jour partielle (PATCH) : seuls les champs fournis dans le corps sont modifiés."
    )
    public ApiResponse<ApplicationResponse> updateApplication(
            @PathVariable UUID appId,
            @Valid @RequestBody UpdateApplicationRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        ApplicationResponse app = applicationService.updateApplication(currentUser.getAccountId(), appId, request);
        return ApiResponse.success("Application mise à jour avec succès.", app);
    }

    // 5. Supprimer (soft delete) une application
    @DeleteMapping("/{appId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Supprimer une application",
            description = "Soft delete : l'application passe au statut DELETED et disparaît de toutes les listes. Cette action est irréversible via l'API."
    )
    public void deleteApplication(
            @PathVariable UUID appId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        applicationService.deleteApplication(currentUser.getAccountId(), appId);
    }
}
