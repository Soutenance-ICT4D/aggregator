package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.AuthProvider;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profil complet du marchand connecté")
public class MerchantProfileResponse {

    @Schema(description = "ID du compte", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Nom complet", example = "DJINE SINTO PAFING")
    private String fullName;

    @Schema(description = "Adresse email", example = "dsintopafing@gmail.com")
    private String email;

    @Schema(description = "Numéro de téléphone", example = "+237695242663")
    private String phone;

    @Schema(description = "Code pays ISO", example = "CM")
    private String country;

    @Schema(description = "URL de l'avatar")
    private String avatarUrl;

    @Schema(description = "Rôle du compte")
    private Role role;

    @Schema(description = "Statut du compte")
    private AccountStatus status;

    @Schema(description = "Niveau de vérification KYC")
    private KycLevel kycLevel;

    @Schema(description = "Fournisseur d'authentification")
    private AuthProvider provider;

    @Schema(description = "Email vérifié")
    private boolean emailVerified;

    @Schema(description = "Téléphone vérifié")
    private boolean phoneVerified;

    @Schema(description = "Date de création du compte")
    private OffsetDateTime createdAt;

    @Schema(description = "Date de dernière mise à jour")
    private OffsetDateTime updatedAt;
}
