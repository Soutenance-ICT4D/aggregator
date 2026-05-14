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
import com.sharepay.aggregator.modules.payment.model.UserBalance;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.modules.payment.service.PayOutService;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
        if (!avatarUrl.startsWith("data:")) return; // URL externe — pas de validation

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

    private String maskAccount(String account) {
        if (account == null || account.length() <= 4) return account;
        return "••••" + account.substring(account.length() - 4);
    }

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
                .payerAccount(maskAccount(t.getPayerAccount()))
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
}
