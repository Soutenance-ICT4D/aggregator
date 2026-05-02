package com.sharepay.aggregator.shared.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.providers.mtn-momo")
public class MtnMomoProperties {

    /** URL de base de l'API (ex: https://api.nokash.app) */
    private String baseUrl;

    /** Clé d'intégration (i_space_key) */
    private String iSpaceKey;

    /** Clé d'application (app_space_key) */
    private String appSpaceKey;

    /** Chemin de l'endpoint pay-in (ex: /lapas-on-trans/trans/api-payin-request/407) */
    private String payinPath;

    /** Chemin de l'endpoint de statut (ex: /lapas-on-trans/trans/310/status-request) */
    private String statusPath;

    /** URL de callback pour les notifications de paiement */
    private String callbackUrl;
}
