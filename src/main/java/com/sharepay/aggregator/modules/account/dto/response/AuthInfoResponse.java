package com.sharepay.aggregator.modules.account.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthInfoResponse {
    @Schema(description = "ID du compte", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID accountId;
    @Schema(description = "Adresse email de l'utilisateur", example = "dsintopafing@gmail.com")
    private String email;
    @Schema(description = "Nom complet de l'utilisateur", example = "DJINE SINTO PAFING")
    private String fullName;
    @Schema(description = "Rôle de l'utilisateur", example = "MERCHANT")
    private Role role;
    @Schema(description = "Statut du compte", example = "PENDING_VERIFICATION")
    private AccountStatus status;
    @Schema(description = "Niveau de KYC de l'utilisateur", example = "NONE")
    private KycLevel kycLevel;
}