package com.sharepay.aggregator.modules.admin.dto.response;

import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vue « 360 » d'un marchand pour l'admin : infos du compte, soldes, statistiques
 * agrégées par statut (pay-in et pay-out) et l'intégralité de ses mouvements inline.
 *
 * <p>Le payload n'est pas paginé : il convient au périmètre actuel (volumes de démo).
 * Si un marchond accumule des milliers de transactions, il faudra passer les listes
 * en endpoints paginés dédiés.</p>
 */
@Data
@Builder
public class Merchant360Response {

    // --- Identité & état ---
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String country;
    private String avatarUrl;
    private AccountStatus status;
    private KycLevel kycLevel;
    private boolean emailVerified;
    private boolean phoneVerified;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // --- Soldes (une entrée par devise) ---
    private List<BalanceInfo> balances;

    // --- Statistiques agrégées ---
    private TransactionStats payInStats;
    private TransactionStats payOutStats;

    // --- Mouvements inline ---
    private List<TransactionInInfo> transactionsIn;
    private List<PayoutInfo> payouts;

    @Data
    @Builder
    public static class BalanceInfo {
        private String currency;
        private Long availableAmount;
        private Long pendingAmount;
        private OffsetDateTime updatedAt;
    }

    /**
     * Compteurs et montants par statut. {@code byStatus} contient une entrée pour
     * chaque valeur de {@link TransactionStatus} (zéro compris) afin que le front
     * puisse afficher tous les statuts sans se soucier des trous.
     */
    @Data
    @Builder
    public static class TransactionStats {
        private long totalCount;
        private long totalVolume;
        private Map<TransactionStatus, StatusBucket> byStatus;
    }

    @Data
    @Builder
    public static class StatusBucket {
        private long count;
        private long sumAmount;
    }

    @Data
    @Builder
    public static class TransactionInInfo {
        private UUID id;
        private String reference;
        private TransactionInType type;
        private TransactionStatus status;
        private Long amount;
        private Long feeAmount;
        private Long netAmount;
        private String currency;
        private String providerName;
        private String payerAccount;
        private String customerName;
        private String failureCode;
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    public static class PayoutInfo {
        private UUID id;
        private String reference;
        private TransactionStatus status;
        private Long amount;
        private Long feeAmount;
        private Long netAmount;
        private String currency;
        private String providerName;
        private String beneficiaryName;
        private String beneficiaryAccount;
        private String failureCode;
        private OffsetDateTime createdAt;
    }
}
