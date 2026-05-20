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

    /**
     * Calcule le montant NET à envoyer au bénéficiaire quand on veut débiter exactement
     * {@code gross} du solde (frais inclus dans gross, non ajoutés par-dessus).
     *
     * Formule : fee = net * rate/100 + fixed  →  net = (gross - fixed) * 100 / (100 + rate)
     */
    public long computeNetFromGross(PaymentProvider provider, long gross) {
        BigDecimal rate  = provider.getFeePercentage() != null ? provider.getFeePercentage() : BigDecimal.ZERO;
        long       fixed = provider.getFeeFixed()      != null ? provider.getFeeFixed()      : 0L;

        long grossMinusFixed = gross - fixed;
        if (grossMinusFixed <= 0) return 0;

        BigDecimal net = BigDecimal.valueOf(grossMinusFixed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100).add(rate), 0, RoundingMode.FLOOR);

        return Math.max(0, net.longValue());
    }
}
