package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vue d'ensemble plateforme pour le tableau de bord admin.
 *
 * <p>Les KPIs sont calculés par des requêtes agrégées SQL (SUM/COUNT) — jamais en
 * chargeant les collections en mémoire — pour rester efficaces à l'échelle plateforme.
 * Les listes « à traiter » sont volontairement courtes (5 à 8 éléments).</p>
 */
@Data
@Builder
public class AdminOverviewResponse {

    // --- KPIs plateforme ---
    private long payInVolume;      // somme des pay-in SUCCESS
    private long payOutVolume;     // somme des pay-out SUCCESS
    private long floatAvailable;   // somme des soldes disponibles (argent détenu)
    private long floatPending;     // somme des soldes en attente
    private long merchantsTotal;
    private long merchantsActive;
    private long merchantsPending; // en attente de vérification (file KYB)
    private long txTotal;          // nombre total de pay-in
    private long txSuccess;        // nombre de pay-in SUCCESS

    // --- À traiter ---
    private List<MerchantMini> pendingVerification;
    private List<MerchantMini> recentMerchants;
    private List<TxMini> recentTransactions;

    @Data
    @Builder
    public static class MerchantMini {
        private UUID id;
        private String fullName;
        private String email;
        private AccountStatus status;
        private KycLevel kycLevel;
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    public static class TxMini {
        private UUID id;
        private String reference;
        private String merchantName;
        private TransactionStatus status;
        private Long amount;
        private String currency;
        private OffsetDateTime createdAt;
    }
}
