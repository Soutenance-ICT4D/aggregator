package com.sharepay.aggregator.modules.admin.controller;

import com.sharepay.aggregator.modules.admin.dto.request.UpsertProviderRequest;
import com.sharepay.aggregator.modules.admin.dto.response.AdminProviderResponse;
import com.sharepay.aggregator.modules.admin.service.AdminProviderService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/providers")
@Tag(name = "Providers", description = "Gestion des opérateurs de paiement")
@SecurityRequirement(name = "bearerAuth")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    @GetMapping
    @Operation(summary = "Lister tous les providers (actifs et inactifs)")
    public ApiResponse<List<AdminProviderResponse>> list() {
        return ApiResponse.success(adminProviderService.listProviders());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un nouveau provider de paiement")
    public ApiResponse<AdminProviderResponse> create(@Valid @RequestBody UpsertProviderRequest request) {
        return ApiResponse.success(adminProviderService.createProvider(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un provider (frais, limites, nom…)")
    public ApiResponse<AdminProviderResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertProviderRequest request
    ) {
        return ApiResponse.success(adminProviderService.updateProvider(id, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activer ou désactiver un provider")
    public ApiResponse<AdminProviderResponse> toggle(@PathVariable UUID id) {
        return ApiResponse.success(adminProviderService.toggleStatus(id));
    }
}
