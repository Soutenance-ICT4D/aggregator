package com.sharepay.aggregator.shared.gateway;

import com.sharepay.aggregator.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registre automatique de tous les {@link PaymentGateway} déclarés comme beans Spring.
 * Ajouter un nouveau provider = créer un @Component qui implémente PaymentGateway.
 */
@Slf4j
@Component
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> gateways;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.gateways = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::getProviderCode, g -> g));
        log.info("Gateways de paiement enregistrés : {}", this.gateways.keySet());
    }

    /**
     * Résout le gateway correspondant au code provider.
     *
     * @throws BusinessException si aucun gateway ne supporte ce provider
     */
    public PaymentGateway resolve(String providerCode) {
        return Optional.ofNullable(gateways.get(providerCode))
                .orElseThrow(() -> new BusinessException(
                        "Provider '" + providerCode + "' non supporté par l'agrégateur.",
                        HttpStatus.BAD_REQUEST,
                        "PROVIDER_NOT_SUPPORTED"
                ));
    }
}
