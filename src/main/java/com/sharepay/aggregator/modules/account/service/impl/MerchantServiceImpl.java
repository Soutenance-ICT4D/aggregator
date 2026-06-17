package com.sharepay.aggregator.modules.account.service.impl;

import com.sharepay.aggregator.modules.account.dto.request.ChangePasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.UpdateProfileRequest;
import com.sharepay.aggregator.modules.account.dto.response.*;
import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.modules.payment.dto.request.TransferRequest;
import com.sharepay.aggregator.modules.payment.dto.response.TransferResponse;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.modules.payment.model.UserBalance;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInSpec;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionOutSpec;
import com.sharepay.aggregator.modules.account.dto.response.TransactionStatsResponse;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.constant.TxChartCustomType;
import com.sharepay.aggregator.shared.constant.TxChartInterval;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantServiceImpl implements MerchantService {

    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final TransactionInRepository transactionInRepository;
    private final TransactionOutRepository transactionOutRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PayOutService payOutService;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ALLOWED_AVATAR_PREFIXES = List.of(
            "data:image/jpeg;base64,",
            "data:image/jpg;base64,",
            "data:image/png;base64,",
            "data:image/webp;base64,"
    );
    private static final long MAX_AVATAR_BYTES = 1_048_576L; // 1 Mo

    // ── Profil ────────────────────────────────────────────────────────────────

    @Override
    public MerchantProfileResponse getProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public MerchantProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getFullName()  != null) user.setFullName(request.getFullName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        if (request.getCountry()   != null) user.setCountry(request.getCountry());
        if (request.getAvatarUrl() != null) {
            if (request.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(null);
            } else {
                validateAvatarDataUri(request.getAvatarUrl());
                user.setAvatarUrl(request.getAvatarUrl());
            }
        }

        return toProfileResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (user.getPasswordHash() == null) {
            throw new BusinessException(
                "Impossible de changer le mot de passe d'un compte OAuth",
                HttpStatus.BAD_REQUEST,
                "OAUTH_ACCOUNT"
            );
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                "Mot de passe actuel incorrect",
                HttpStatus.BAD_REQUEST,
                "INVALID_CURRENT_PASSWORD"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ── Helpers profil ────────────────────────────────────────────────────────

    private void validateAvatarDataUri(String avatarUrl) {
        if (!avatarUrl.startsWith("data:")) return; // URL externe - pas de validation

        boolean validPrefix = ALLOWED_AVATAR_PREFIXES.stream().anyMatch(avatarUrl::startsWith);
        if (!validPrefix) {
            throw new BusinessException(
                "Format d'image non supporté. Utilisez jpg, png ou webp.",
                HttpStatus.BAD_REQUEST, "INVALID_IMAGE_FORMAT"
            );
        }

        int dataStart = avatarUrl.indexOf(',') + 1;
        long approximateBytes = (long) ((avatarUrl.length() - dataStart) * 0.75);
        if (approximateBytes > MAX_AVATAR_BYTES) {
            throw new BusinessException(
                "L'image ne doit pas dépasser 1 Mo.",
                HttpStatus.BAD_REQUEST, "IMAGE_TOO_LARGE"
            );
        }
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouvé",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));
    }

    private MerchantProfileResponse toProfileResponse(User user) {
        return MerchantProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .country(user.getCountry())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .kycLevel(user.getKycLevel())
                .provider(user.getProvider())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // ── Balances ──────────────────────────────────────────────────────────────

    @Override
    public List<UserBalanceResponse> getBalances(UUID userId) {
        return userBalanceRepository.findByUser_IdOrderByCurrencyAsc(userId)
                .stream()
                .map(b -> UserBalanceResponse.builder()
                        .id(b.getId())
                        .currency(b.getCurrency())
                        .availableAmount(b.getAvailableAmount())
                        .pendingAmount(b.getPendingAmount())
                        .updatedAt(b.getUpdatedAt())
                        .build())
                .toList();
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Override
    public MerchantDashboardResponse getDashboard(UUID userId) {
        UserBalance balance = userBalanceRepository.findByUser_IdAndCurrency(userId, "XAF")
                .orElseGet(() -> UserBalance.builder()
                        .availableAmount(0L)
                        .pendingAmount(0L)
                        .currency("XAF")
                        .build());

        OffsetDateTime startOfDay = OffsetDateTime.now(ZoneOffset.UTC)
                .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        Long dailyVolume = transactionInRepository.sumDailyVolume(userId, startOfDay, endOfDay);
        List<TransactionIn> todayTx = transactionInRepository.findTodayByUser(userId, startOfDay, endOfDay);
        List<TransactionIn> lastFive = transactionInRepository.findRecentByUser(userId, PageRequest.of(0, 5));

        return MerchantDashboardResponse.builder()
                .availableBalance(balance.getAvailableAmount())
                .pendingBalance(balance.getPendingAmount())
                .currency(balance.getCurrency())
                .dailyVolume(dailyVolume)
                .todayTransactionCount(todayTx.size())
                .todayTransactions(todayTx.stream().map(this::toSummary).toList())
                .lastFiveTransactions(lastFive.stream().map(this::toSummary).toList())
                .build();
    }

    // ── Chart ─────────────────────────────────────────────────────────────────

    @Override
    public TransactionChartResponse getTransactionChart(UUID userId, ChartInterval interval, ChartGroupBy groupBy) {
        String currency = userBalanceRepository.findByUser_IdAndCurrency(userId, "XAF")
                .map(UserBalance::getCurrency)
                .orElse("XAF");

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startOfToday = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);

        OffsetDateTime from = switch (interval) {
            case TODAY       -> startOfToday;
            case LAST_7_DAYS -> startOfToday.minusDays(6);
            case LAST_30_DAYS -> startOfToday.minusDays(29);
        };
        OffsetDateTime to = startOfToday.plusDays(1);

        List<TransactionIn> transactions = transactionInRepository.findForChartByUser(userId, from, to);

        // ── Slots temporels ──────────────────────────────────────────────────
        List<String> labels = new ArrayList<>();
        List<OffsetDateTime> slotStarts = new ArrayList<>();

        if (interval == ChartInterval.TODAY) {
            for (int h = 0; h < 24; h++) {
                slotStarts.add(startOfToday.withHour(h));
                labels.add(String.format("%02d:00", h));
            }
        } else {
            int days = interval == ChartInterval.LAST_7_DAYS ? 7 : 30;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);
            for (int d = 0; d < days; d++) {
                OffsetDateTime slot = from.plusDays(d);
                slotStarts.add(slot);
                labels.add(slot.format(fmt));
            }
        }

        // ── Clé de groupement ────────────────────────────────────────────────
        Function<TransactionIn, String> keyFn = switch (groupBy) {
            case STATUS      -> t -> t.getStatus().name();
            case PROVIDER    -> t -> t.getPaymentProvider() != null
                                        ? t.getPaymentProvider().getName()
                                        : "Inconnu";
            case APPLICATION -> t -> t.getApplication().getName();
        };

        // Clés de groupement : toujours complètes même si la période est vide
        Set<String> groupKeys;
        if (groupBy == ChartGroupBy.STATUS) {
            // Ordre canonique : les 5 statuts toujours présents
            groupKeys = new LinkedHashSet<>(List.of("SUCCESS", "PENDING", "FAILED", "CANCELLED", "REFUNDED"));
        } else if (groupBy == ChartGroupBy.PROVIDER) {
            // Tous les opérateurs actifs du système (toujours affichés, même sans transaction)
            groupKeys = paymentProviderRepository.findByIsActiveTrueOrderByNameAsc()
                    .stream()
                    .map(PaymentProvider::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            // APPLICATION : toutes les apps historiques du marchand
            List<String> historical = transactionInRepository.findDistinctApplicationNamesByUser(userId);
            groupKeys = new LinkedHashSet<>(historical);
            transactions.stream().map(keyFn).forEach(groupKeys::add);
        }

        // ── Construction des séries ──────────────────────────────────────────
        List<ChartSeriesItem> series = new ArrayList<>();

        for (String key : groupKeys) {
            List<TransactionIn> groupTxs = transactions.stream()
                    .filter(t -> keyFn.apply(t).equals(key))
                    .toList();

            List<Long> counts  = new ArrayList<>();
            List<Long> volumes = new ArrayList<>();

            for (int i = 0; i < slotStarts.size(); i++) {
                OffsetDateTime slotStart = slotStarts.get(i);
                OffsetDateTime slotEnd   = (i < slotStarts.size() - 1) ? slotStarts.get(i + 1) : to;

                List<TransactionIn> slotTxs = groupTxs.stream()
                        .filter(t -> !t.getCreatedAt().isBefore(slotStart)
                                  &&  t.getCreatedAt().isBefore(slotEnd))
                        .toList();

                counts.add((long) slotTxs.size());
                volumes.add(slotTxs.stream().mapToLong(TransactionIn::getNetAmount).sum());
            }

            series.add(ChartSeriesItem.builder()
                    .key(key)
                    .counts(counts)
                    .volumes(volumes)
                    .build());
        }

        return TransactionChartResponse.builder()
                .interval(interval)
                .groupBy(groupBy)
                .currency(currency)
                .labels(labels)
                .series(series)
                .build();
    }

    // ── Transactions paginées ─────────────────────────────────────────────────

    @Override
    public PaginationResponse<TransactionSummaryResponse> getTransactions(
            UUID userId, int page, int size,
            TransactionStatus status, TransactionInType type
    ) {
        Page<TransactionIn> result = transactionInRepository.findPagedByUser(
                userId, status, type, PageRequest.of(page, size)
        );
        return PaginationResponse.<TransactionSummaryResponse>builder()
                .content(result.getContent().stream().map(this::toSummary).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TransactionSummaryResponse toSummary(TransactionIn t) {
        return TransactionSummaryResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .merchantReference(t.getMerchantReference())
                .type(t.getType())
                .amount(t.getAmount())
                .feeAmount(t.getFeeAmount())
                .netAmount(t.getNetAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .description(t.getDescription())
                .provider(t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : null)
                .payerAccount(t.getPayerAccount())
                .payerName(t.getPayerName())
                .payerEmail(t.getPayerEmail())
                .appName(t.getApplication() != null ? t.getApplication().getName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    // ── Providers ─────────────────────────────────────────────────────────────

    @Override
    public List<PaymentProviderResponse> getWithdrawalProviders() {
        return paymentProviderRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(p -> PaymentProviderResponse.builder()
                        .code(p.getCode())
                        .name(p.getName())
                        .type(p.getType().name())
                        .currency(p.getCurrency())
                        .minAmount(p.getMinAmount())
                        .maxAmount(p.getMaxAmount())
                        .build())
                .toList();
    }

    // ── Withdrawal ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransferResponse initiateWithdrawal(UUID userId, TransferRequest request) {
        return payOutService.createTransferByUserId(userId, request);
    }

    // ── Transactions-in (nouveaux endpoints) ──────────────────────────────────

    @Override
    public TransactionStatsResponse getTransactionInStats(UUID userId) {
        long total     = transactionInRepository.countByApplication_User_IdAndApplication_StatusNot(userId, AppStatus.DELETED);
        long success   = transactionInRepository.countByApplication_User_IdAndApplication_StatusNotAndStatus(userId, AppStatus.DELETED, TransactionStatus.SUCCESS);
        long pending   = transactionInRepository.countByApplication_User_IdAndApplication_StatusNotAndStatus(userId, AppStatus.DELETED, TransactionStatus.PENDING);
        long failed    = transactionInRepository.countByApplication_User_IdAndApplication_StatusNotAndStatus(userId, AppStatus.DELETED, TransactionStatus.FAILED);
        long cancelled = transactionInRepository.countByApplication_User_IdAndApplication_StatusNotAndStatus(userId, AppStatus.DELETED, TransactionStatus.CANCELLED);
        return TransactionStatsResponse.builder()
                .total(total).successCount(success).pendingCount(pending)
                .failedCount(failed).cancelledCount(cancelled).build();
    }

    @Override
    public PaginationResponse<TransactionSummaryResponse> getTransactionsIn(
            UUID userId, int page, int size,
            TransactionStatus status, TransactionInType type,
            UUID appId, OffsetDateTime from, OffsetDateTime to
    ) {
        Page<TransactionIn> result = transactionInRepository.findAll(
                TransactionInSpec.forMerchant(userId, status, type, appId, from, to),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PaginationResponse.<TransactionSummaryResponse>builder()
                .content(result.getContent().stream().map(this::toSummary).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public TransactionInDetailResponse getTransactionInDetail(UUID userId, UUID id) {
        TransactionIn t = transactionInRepository.findByIdForMerchant(id, userId)
                .orElseThrow(() -> new BusinessException(
                        "Transaction introuvable", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND"));
        return TransactionInDetailResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .merchantReference(t.getMerchantReference())
                .type(t.getType())
                .amount(t.getAmount())
                .feeAmount(t.getFeeAmount())
                .netAmount(t.getNetAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .description(t.getDescription())
                .provider(t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : null)
                .providerTransactionId(t.getProviderTransactionId())
                .appId(t.getApplication() != null ? t.getApplication().getId() : null)
                .appName(t.getApplication() != null ? t.getApplication().getName() : null)
                .customerName(t.getCustomerName())
                .customerEmail(t.getCustomerEmail())
                .customerPhone(t.getCustomerPhone())
                .payerAccount(t.getPayerAccount())
                .payerName(t.getPayerName())
                .payerEmail(t.getPayerEmail())
                .successUrl(t.getSuccessUrl())
                .cancelUrl(t.getCancelUrl())
                .failureReason(t.getFailureReason())
                .failureCode(t.getFailureCode())
                .expiresAt(t.getExpiresAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    @Override
    public TxChartResponse getTransactionInChart(
            UUID userId, TxChartInterval interval, TxChartCustomType customType,
            Integer year, LocalDate fromDate, LocalDate toDate, ChartGroupBy groupBy
    ) {
        String currency = userBalanceRepository.findByUser_IdAndCurrency(userId, "XAF")
                .map(UserBalance::getCurrency).orElse("XAF");

        ChartBounds bounds = buildChartBounds(interval, customType, year, fromDate, toDate);
        List<TransactionIn> transactions = transactionInRepository.findForChartByUser(userId, bounds.from, bounds.to);

        Function<TransactionIn, String> keyFn = switch (groupBy) {
            case STATUS      -> t -> t.getStatus().name();
            case PROVIDER    -> t -> t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : "Inconnu";
            case APPLICATION -> t -> t.getApplication() != null ? t.getApplication().getName() : "Inconnu";
        };

        Set<String> groupKeys = buildInGroupKeys(groupBy, userId, transactions, keyFn);

        List<ChartSeriesItem> series = new ArrayList<>();
        for (String key : groupKeys) {
            List<TransactionIn> groupTxs = transactions.stream()
                    .filter(t -> keyFn.apply(t).equals(key)).toList();
            List<Long> counts = new ArrayList<>();
            List<Long> volumes = new ArrayList<>();
            for (int i = 0; i < bounds.slotStarts.size(); i++) {
                OffsetDateTime slotStart = bounds.slotStarts.get(i);
                OffsetDateTime slotEnd = (i < bounds.slotStarts.size() - 1) ? bounds.slotStarts.get(i + 1) : bounds.to;
                List<TransactionIn> slotTxs = groupTxs.stream()
                        .filter(t -> !t.getCreatedAt().isBefore(slotStart) && t.getCreatedAt().isBefore(slotEnd))
                        .toList();
                counts.add((long) slotTxs.size());
                volumes.add(slotTxs.stream().mapToLong(TransactionIn::getNetAmount).sum());
            }
            series.add(ChartSeriesItem.builder().key(key).counts(counts).volumes(volumes).build());
        }

        return TxChartResponse.builder()
                .interval(interval).customType(customType).groupBy(groupBy)
                .currency(currency).labels(bounds.labels).series(series).build();
    }

    private Set<String> buildInGroupKeys(ChartGroupBy groupBy, UUID userId,
                                         List<TransactionIn> transactions,
                                         Function<TransactionIn, String> keyFn) {
        if (groupBy == ChartGroupBy.STATUS) {
            return new LinkedHashSet<>(List.of("SUCCESS", "PENDING", "FAILED", "CANCELLED", "REFUNDED"));
        }
        if (groupBy == ChartGroupBy.PROVIDER) {
            return paymentProviderRepository.findByIsActiveTrueOrderByNameAsc().stream()
                    .map(PaymentProvider::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        List<String> historical = transactionInRepository.findDistinctApplicationNamesByUser(userId);
        Set<String> keys = new LinkedHashSet<>(historical);
        transactions.stream().map(keyFn).forEach(keys::add);
        return keys;
    }

    // ── Transactions-out (nouveaux endpoints) ─────────────────────────────────

    @Override
    public TransactionStatsResponse getTransactionOutStats(UUID userId) {
        long total     = transactionOutRepository.countByUser_Id(userId);
        long success   = transactionOutRepository.countByUser_IdAndStatus(userId, TransactionStatus.SUCCESS);
        long pending   = transactionOutRepository.countByUser_IdAndStatus(userId, TransactionStatus.PENDING);
        long failed    = transactionOutRepository.countByUser_IdAndStatus(userId, TransactionStatus.FAILED);
        long cancelled = transactionOutRepository.countByUser_IdAndStatus(userId, TransactionStatus.CANCELLED);
        return TransactionStatsResponse.builder()
                .total(total).successCount(success).pendingCount(pending)
                .failedCount(failed).cancelledCount(cancelled).build();
    }

    @Override
    public PaginationResponse<TransactionOutSummaryResponse> getTransactionsOut(
            UUID userId, int page, int size,
            TransactionStatus status, UUID appId,
            OffsetDateTime from, OffsetDateTime to
    ) {
        Page<TransactionOut> result = transactionOutRepository.findAll(
                TransactionOutSpec.forMerchant(userId, status, appId, from, to),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PaginationResponse.<TransactionOutSummaryResponse>builder()
                .content(result.getContent().stream().map(this::toOutSummary).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public TransactionOutDetailResponse getTransactionOutDetail(UUID userId, UUID id) {
        TransactionOut t = transactionOutRepository.findByIdForMerchant(id, userId)
                .orElseThrow(() -> new BusinessException(
                        "Transaction introuvable", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND"));
        return TransactionOutDetailResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .merchantReference(t.getMerchantReference())
                .amount(t.getAmount())
                .feeAmount(t.getFeeAmount())
                .netAmount(t.getNetAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .description(t.getDescription())
                .provider(t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : null)
                .providerTransactionId(t.getProviderTransactionId())
                .appId(t.getApplication() != null ? t.getApplication().getId() : null)
                .appName(t.getApplication() != null ? t.getApplication().getName() : null)
                .beneficiaryName(t.getBeneficiaryName())
                .beneficiaryEmail(t.getBeneficiaryEmail())
                .beneficiaryAccount(t.getBeneficiaryAccount())
                .failureReason(t.getFailureReason())
                .failureCode(t.getFailureCode())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    @Override
    public TxChartResponse getTransactionOutChart(
            UUID userId, TxChartInterval interval, TxChartCustomType customType,
            Integer year, LocalDate fromDate, LocalDate toDate, ChartGroupBy groupBy
    ) {
        String currency = userBalanceRepository.findByUser_IdAndCurrency(userId, "XAF")
                .map(UserBalance::getCurrency).orElse("XAF");

        ChartBounds bounds = buildChartBounds(interval, customType, year, fromDate, toDate);
        List<TransactionOut> transactions = transactionOutRepository.findForChartByUser(userId, bounds.from, bounds.to);

        Function<TransactionOut, String> keyFn = switch (groupBy) {
            case STATUS      -> t -> t.getStatus().name();
            case PROVIDER    -> t -> t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : "Inconnu";
            case APPLICATION -> t -> t.getApplication() != null ? t.getApplication().getName() : "Direct";
        };

        Set<String> groupKeys = buildOutGroupKeys(groupBy, userId, transactions, keyFn);

        List<ChartSeriesItem> series = new ArrayList<>();
        for (String key : groupKeys) {
            List<TransactionOut> groupTxs = transactions.stream()
                    .filter(t -> keyFn.apply(t).equals(key)).toList();
            List<Long> counts = new ArrayList<>();
            List<Long> volumes = new ArrayList<>();
            for (int i = 0; i < bounds.slotStarts.size(); i++) {
                OffsetDateTime slotStart = bounds.slotStarts.get(i);
                OffsetDateTime slotEnd = (i < bounds.slotStarts.size() - 1) ? bounds.slotStarts.get(i + 1) : bounds.to;
                List<TransactionOut> slotTxs = groupTxs.stream()
                        .filter(t -> !t.getCreatedAt().isBefore(slotStart) && t.getCreatedAt().isBefore(slotEnd))
                        .toList();
                counts.add((long) slotTxs.size());
                volumes.add(slotTxs.stream().mapToLong(TransactionOut::getNetAmount).sum());
            }
            series.add(ChartSeriesItem.builder().key(key).counts(counts).volumes(volumes).build());
        }

        return TxChartResponse.builder()
                .interval(interval).customType(customType).groupBy(groupBy)
                .currency(currency).labels(bounds.labels).series(series).build();
    }

    private Set<String> buildOutGroupKeys(ChartGroupBy groupBy, UUID userId,
                                          List<TransactionOut> transactions,
                                          Function<TransactionOut, String> keyFn) {
        if (groupBy == ChartGroupBy.STATUS) {
            return new LinkedHashSet<>(List.of("SUCCESS", "PENDING", "FAILED", "CANCELLED", "REFUNDED"));
        }
        if (groupBy == ChartGroupBy.PROVIDER) {
            return paymentProviderRepository.findByIsActiveTrueOrderByNameAsc().stream()
                    .map(PaymentProvider::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        List<String> historical = transactionOutRepository.findDistinctApplicationNamesByUser(userId);
        Set<String> keys = new LinkedHashSet<>(historical);
        transactions.stream().map(keyFn).forEach(keys::add);
        return keys;
    }

    private TransactionOutSummaryResponse toOutSummary(TransactionOut t) {
        return TransactionOutSummaryResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .merchantReference(t.getMerchantReference())
                .amount(t.getAmount())
                .feeAmount(t.getFeeAmount())
                .netAmount(t.getNetAmount())
                .currency(t.getCurrency())
                .status(t.getStatus())
                .description(t.getDescription())
                .provider(t.getPaymentProvider() != null ? t.getPaymentProvider().getName() : null)
                .beneficiaryName(t.getBeneficiaryName())
                .beneficiaryAccount(t.getBeneficiaryAccount())
                .appName(t.getApplication() != null ? t.getApplication().getName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    // ── Chart bounds helper ───────────────────────────────────────────────────

    private record ChartBounds(
            OffsetDateTime from, OffsetDateTime to,
            List<String> labels, List<OffsetDateTime> slotStarts
    ) {}

    private ChartBounds buildChartBounds(TxChartInterval interval, TxChartCustomType customType,
                                         Integer year, LocalDate fromDate, LocalDate toDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<String> labels = new ArrayList<>();
        List<OffsetDateTime> slotStarts = new ArrayList<>();
        OffsetDateTime from;
        OffsetDateTime to;

        switch (interval) {
            case TODAY -> {
                from = today.atStartOfDay().atOffset(ZoneOffset.UTC);
                to = from.plusDays(1);
                for (int h = 0; h < 24; h++) {
                    slotStarts.add(from.withHour(h));
                    labels.add(String.format("%02d:00", h));
                }
            }
            case THIS_WEEK -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                from = monday.atStartOfDay().atOffset(ZoneOffset.UTC);
                to = from.plusDays(7);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.FRENCH);
                for (int d = 0; d < 7; d++) {
                    OffsetDateTime slot = from.plusDays(d);
                    slotStarts.add(slot);
                    labels.add(slot.format(fmt));
                }
            }
            case THIS_MONTH -> {
                LocalDate first = today.withDayOfMonth(1);
                from = first.atStartOfDay().atOffset(ZoneOffset.UTC);
                int days = today.lengthOfMonth();
                to = from.plusDays(days);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);
                for (int d = 0; d < days; d++) {
                    OffsetDateTime slot = from.plusDays(d);
                    slotStarts.add(slot);
                    labels.add(slot.format(fmt));
                }
            }
            case CUSTOM -> {
                if (customType == null) {
                    throw new BusinessException(
                            "customType requis pour l'intervalle CUSTOM", HttpStatus.BAD_REQUEST, "MISSING_CUSTOM_TYPE");
                }
                if (customType == TxChartCustomType.YEAR) {
                    if (year == null) {
                        throw new BusinessException(
                                "Le paramètre year est requis pour customType=YEAR", HttpStatus.BAD_REQUEST, "MISSING_YEAR");
                    }
                    from = LocalDate.of(year, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
                    to = LocalDate.of(year + 1, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
                    for (int m = 1; m <= 12; m++) {
                        OffsetDateTime slot = LocalDate.of(year, m, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
                        slotStarts.add(slot);
                        labels.add(slot.format(fmt));
                    }
                } else {
                    if (fromDate == null || toDate == null) {
                        throw new BusinessException(
                                "Les paramètres from et to sont requis pour customType=DAYS",
                                HttpStatus.BAD_REQUEST, "MISSING_DATE_RANGE");
                    }
                    if (fromDate.isAfter(toDate)) {
                        throw new BusinessException(
                                "from doit être antérieur ou égal à to", HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
                    }
                    long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
                    if (daysBetween > 30) {
                        throw new BusinessException(
                                "La plage maximale pour DAYS est de 30 jours", HttpStatus.BAD_REQUEST, "DATE_RANGE_TOO_LARGE");
                    }
                    from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
                    to = toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM", Locale.FRENCH);
                    for (long d = 0; d <= daysBetween; d++) {
                        OffsetDateTime slot = from.plusDays(d);
                        slotStarts.add(slot);
                        labels.add(slot.format(fmt));
                    }
                }
            }
            default -> throw new BusinessException("Intervalle non supporté", HttpStatus.BAD_REQUEST, "INVALID_INTERVAL");
        }

        return new ChartBounds(from, to, labels, slotStarts);
    }
}
