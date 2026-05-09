package com.sharepay.aggregator.modules.payment.service;

import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeCalculatorService {

    public long computeFee(PaymentProvider provider, long amount) {
        BigDecimal fee = BigDecimal.ZERO;
        if (provider.getFeePercentage() != null) {
            fee = fee.add(
                BigDecimal.valueOf(amount)
                    .multiply(provider.getFeePercentage())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            );
        }
        if (provider.getFeeFixed() != null) {
            fee = fee.add(BigDecimal.valueOf(provider.getFeeFixed()));
        }
        return fee.longValue();
    }
}
