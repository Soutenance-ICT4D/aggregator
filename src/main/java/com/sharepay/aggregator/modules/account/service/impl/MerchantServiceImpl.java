package com.sharepay.aggregator.modules.account.service.impl;

import com.sharepay.aggregator.modules.account.dto.response.*;
import com.sharepay.aggregator.modules.account.service.MerchantService;
import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.modules.payment.model.UserBalance;
import com.sharepay.aggregator.modules.payment.repository.PaymentProviderRepository;
import com.sharepay.aggregator.modules.payment.repository.TransactionInRepository;
import com.sharepay.aggregator.modules.payment.repository.UserBalanceRepository;
import com.sharepay.aggregator.shared.constant.ChartGroupBy;
import com.sharepay.aggregator.shared.constant.ChartInterval;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import com.sharepay.aggregator.shared.dto.PaginationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final UserBalanceRepository userBalanceRepository;
    private final TransactionInRepository transactionInRepository;
    private final PaymentProviderRepository paymentProviderRepository;

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
}
