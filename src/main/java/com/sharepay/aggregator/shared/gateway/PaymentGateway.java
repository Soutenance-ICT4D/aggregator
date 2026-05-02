package com.sharepay.aggregator.shared.gateway;

import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInResponse;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutResponse;
import com.sharepay.aggregator.shared.gateway.dto.GatewayStatusResponse;

/**
 * Contrat d'intégration d'un opérateur de paiement.
 * Chaque implémentation correspond à un provider (MTN, Orange…).
 */
public interface PaymentGateway {

    /** Code du provider géré par cette implémentation (ex: "MTN_MOMO_CM") */
    String getProviderCode();

    /** Initie une demande de paiement entrant. */
    GatewayPayInResponse initiatePayIn(GatewayPayInRequest request);

    /** Initie un virement sortant. */
    GatewayPayOutResponse initiatePayOut(GatewayPayOutRequest request);

    /** Interroge le provider pour obtenir le statut courant d'une transaction. */
    GatewayStatusResponse checkStatus(String providerTransactionId);
}
