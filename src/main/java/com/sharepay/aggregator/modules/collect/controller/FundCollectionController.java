package com.sharepay.aggregator.modules.collect.controller;

import com.sharepay.aggregator.modules.collect.dto.request.CreateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.request.UpdateFundCollectionRequest;
import com.sharepay.aggregator.modules.collect.dto.response.FundCollectionResponse;
import com.sharepay.aggregator.modules.collect.service.FundCollectionService;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequiredArgsConstructor
@Tag(name = "Collecte de fonds", description = "Gestion des collectes de fonds marchandes")
@SecurityRequirement(name = "bearerAuth")
public class FundCollectionController {

    private final FundCollectionService fundCollectionService;

    // 1. Créer une collecte pour une application donnée
    @PostMapping("/api/v1/apps/{applicationId}/fund-collections")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Créer une collecte de fonds",
            description = "Crée une nouvelle collecte de fonds pour l'application spécifiée. " +
                    "Un slug unique est généré automatiquement à partir du titre."
    )
    public ApiResponse<FundCollectionResponse> createCollection(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateFundCollectionRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        FundCollectionResponse response = fundCollectionService.createCollection(
                currentUser.getAccountId(), applicationId, request);
        return ApiResponse.success("Collecte créée avec succès.", response);
    }

    // 2. Lister les collectes de l'utilisateur (toutes apps ou filtrées par app)
    @GetMapping("/api/v1/fund-collections")
    @Operation(
            summary = "Lister les collectes de fonds",
            description = "Retourne toutes les collectes du marchand. " +
                    "Filtrable par application (?applicationId=) et par statut (?status=). " +
                    "Les collectes supprimées (DELETED) sont exclues par défaut."
    )
    public ApiResponse<List<FundCollectionResponse>> getCollections(
            @Parameter(description = "Filtrer par application") @RequestParam(required = false) UUID applicationId,
            @Parameter(description = "Filtrer par statut (ACTIVE, CLOSED, EXPIRED)") @RequestParam(required = false) FundCollectionStatus status,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        List<FundCollectionResponse> collections = fundCollectionService.getCollections(
                currentUser.getAccountId(), applicationId, status);
        return ApiResponse.success(collections);
    }

    // 3. Détail d'une collecte
    @GetMapping("/api/v1/fund-collections/{collectionId}")
    @Operation(
            summary = "Détail d'une collecte",
            description = "Retourne les informations complètes d'une collecte appartenant au marchand."
    )
    public ApiResponse<FundCollectionResponse> getCollection(
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        FundCollectionResponse response = fundCollectionService.getCollection(
                currentUser.getAccountId(), collectionId);
        return ApiResponse.success(response);
    }

    // 4. Mettre à jour partiellement une collecte (ACTIVE uniquement)
    @PatchMapping("/api/v1/fund-collections/{collectionId}")
    @Operation(
            summary = "Mettre à jour une collecte",
            description = "Mise à jour partielle (PATCH) des champs d'une collecte. " +
                    "Uniquement possible si la collecte est au statut ACTIVE."
    )
    public ApiResponse<FundCollectionResponse> updateCollection(
            @PathVariable UUID collectionId,
            @Valid @RequestBody UpdateFundCollectionRequest request,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        FundCollectionResponse response = fundCollectionService.updateCollection(
                currentUser.getAccountId(), collectionId, request);
        return ApiResponse.success("Collecte mise à jour avec succès.", response);
    }

    // 5. Clôturer une collecte
    @PatchMapping("/api/v1/fund-collections/{collectionId}/close")
    @Operation(
            summary = "Clôturer une collecte",
            description = "Passe la collecte au statut CLOSED. " +
                    "Applicable aux collectes ACTIVE et EXPIRED."
    )
    public ApiResponse<FundCollectionResponse> closeCollection(
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        FundCollectionResponse response = fundCollectionService.closeCollection(
                currentUser.getAccountId(), collectionId);
        return ApiResponse.success("Collecte clôturée avec succès.", response);
    }

    // 6. Réouvrir une collecte
    @PatchMapping("/api/v1/fund-collections/{collectionId}/reopen")
    @Operation(
            summary = "Réouvrir une collecte",
            description = "Passe la collecte de CLOSED à ACTIVE. " +
                    "Si la date d'expiration est dépassée, la collecte passe en EXPIRED et son nouvel état est retourné."
    )
    public ApiResponse<FundCollectionResponse> reopenCollection(
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        FundCollectionResponse response = fundCollectionService.reopenCollection(
                currentUser.getAccountId(), collectionId);
        return ApiResponse.success(response);
    }

    // 7. Accéder publiquement à une collecte par slug (sans authentification)
    @GetMapping("/api/v1/public/fund-collections/{slug}")
    @Operation(
            summary = "Accéder à une collecte par son slug (public)",
            description = "Retourne les informations publiques d'une collecte active. " +
                    "Accessible sans authentification. " +
                    "Retourne 404 si la collecte est inexistante, clôturée, expirée ou supprimée.",
            security = {}
    )
    public ApiResponse<FundCollectionResponse> getPublicCollection(@PathVariable String slug) {
        FundCollectionResponse response = fundCollectionService.getPublicCollection(slug);
        return ApiResponse.success(response);
    }

    // 8. Supprimer (soft delete) une collecte
    @DeleteMapping("/api/v1/fund-collections/{collectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Supprimer une collecte",
            description = "Soft delete : la collecte passe au statut DELETED. Cette action est irréversible via l'API."
    )
    public void deleteCollection(
            @PathVariable UUID collectionId,
            @AuthenticationPrincipal AuthentificatedUser currentUser
    ) {
        fundCollectionService.deleteCollection(currentUser.getAccountId(), collectionId);
    }
}
