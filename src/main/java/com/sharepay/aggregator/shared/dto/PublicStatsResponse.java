package com.sharepay.aggregator.shared.dto;

public record PublicStatsResponse(
        long merchantCount,
        long transactionCount,
        long paymentMethodCount
) {}
