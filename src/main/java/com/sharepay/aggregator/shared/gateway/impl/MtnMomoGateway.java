package com.sharepay.aggregator.shared.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.gateway.PaymentGateway;
import com.sharepay.aggregator.shared.gateway.config.MtnMomoProperties;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayInResponse;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutRequest;
import com.sharepay.aggregator.shared.gateway.dto.GatewayPayOutResponse;
import com.sharepay.aggregator.shared.gateway.dto.GatewayStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MtnMomoGateway implements PaymentGateway {

    private static final String PROVIDER_CODE = "MTN_MOMO_CM";
    private static final String PAYMENT_METHOD = "MTN_MOMO";
    private static final String PAYOUT_PATH = "/lapas-on-trans/trans/api-payout-request/407";
    private static final String AUTH_PATH = "/lapas-on-trans/trans/auth";

    private final MtnMomoProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public GatewayPayInResponse initiatePayIn(GatewayPayInRequest request) {
        Map<String, Object> body = Map.of(
                "i_space_key",    props.getISpaceKey(),
                "app_space_key",  props.getAppSpaceKey(),
                "payment_type",   "CM_MOBILEMONEY",
                "country",        "CM",
                "payment_method", PAYMENT_METHOD,
                "order_id",       request.getReference(),
                "amount",         String.valueOf(request.getAmount().longValue()),
                "user_data",      Map.of("user_phone", sanitizePhone(request.getPayerAccount()))
        );

        try {
            log.info("[MTN] Initiation pay-in - orderId: {}, montant: {}", request.getReference(), request.getAmount());

            String raw = restClient.post()
                    .uri(props.getBaseUrl() + props.getPayinPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String status = root.path("status").asText();
            String message = root.path("message").asText();
            JsonNode data = root.path("data");
            String providerTxId = data.path("id").asText(null);

            log.info("[MTN] Réponse pay-in - status: {}, txId: {}", status, providerTxId);

            return GatewayPayInResponse.builder()
                    .providerTransactionId(providerTxId)
                    .status(mapToGatewayStatus(data.path("status").asText("PENDING")))
                    .message(message)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("[MTN] Echec pay-in - orderId: {} - {}", request.getReference(), e.getMessage());
            throw new BusinessException("Echec de l'initiation du paiement MTN MoMo : " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, "MTN_GATEWAY_ERROR");
        } catch (Exception e) {
            log.error("[MTN] Erreur inattendue pay-in - {}", e.getMessage());
            throw new BusinessException("Erreur interne MTN gateway.", HttpStatus.INTERNAL_SERVER_ERROR, "MTN_INTERNAL_ERROR");
        }
    }

    @Override
    public GatewayPayOutResponse initiatePayOut(GatewayPayOutRequest request) {
        String authToken = authenticate();

        Map<String, Object> body = Map.of(
                "i_space_key",    props.getISpaceKey(),
                "app_space_key",  props.getAppSpaceKey(),
                "payment_type",   "CM_MOBILEMONEY",
                "country",        "CM",
                "payment_method", PAYMENT_METHOD,
                "order_id",       request.getReference(),
                "amount",         String.valueOf(request.getAmount().longValue()),
                "callback_url",   props.getCallbackUrl(),
                "user_data",      Map.of("user_phone", Long.parseLong(sanitizePhone(request.getBeneficiaryAccount())))
        );

        try {
            log.info("[MTN] Initiation pay-out - orderId: {}, montant: {}", request.getReference(), request.getAmount());

            String raw = restClient.post()
                    .uri(props.getBaseUrl() + PAYOUT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("auth-code", authToken)
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String message = root.path("message").asText();
            JsonNode data = root.path("data");
            String providerTxId = data.path("id").asText(null);

            log.info("[MTN] Réponse pay-out - txId: {}", providerTxId);

            return GatewayPayOutResponse.builder()
                    .providerTransactionId(providerTxId)
                    .status(mapToGatewayStatus(data.path("status").asText("PENDING")))
                    .message(message)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("[MTN] Echec pay-out - orderId: {} - {}", request.getReference(), e.getMessage());
            throw new BusinessException("Echec de l'initiation du virement MTN MoMo : " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, "MTN_GATEWAY_ERROR");
        } catch (Exception e) {
            log.error("[MTN] Erreur inattendue pay-out - {}", e.getMessage());
            throw new BusinessException("Erreur interne MTN gateway.", HttpStatus.INTERNAL_SERVER_ERROR, "MTN_INTERNAL_ERROR");
        }
    }

    @Override
    public GatewayStatusResponse checkStatus(String providerTransactionId) {
        String url = props.getBaseUrl() + props.getStatusPath() + "?transaction_id=" + providerTransactionId;
        try {
            String raw = restClient.post()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root   = objectMapper.readTree(raw);
            JsonNode data   = root.path("data");
            String   status = data.path("status").asText("PENDING");
            String   message = root.path("message").asText();

            log.info("[MTN] checkStatus - txId: {}, status: {}", providerTransactionId, status);

            return GatewayStatusResponse.builder()
                    .providerTransactionId(providerTransactionId)
                    .status(mapToGatewayStatus(status))
                    .message(message)
                    .build();

        } catch (Exception e) {
            log.error("[MTN] Erreur checkStatus - txId: {} - {}", providerTransactionId, e.getMessage());
            return GatewayStatusResponse.builder()
                    .providerTransactionId(providerTransactionId)
                    .status("PENDING")
                    .message("Vérification du statut échouée, nouvelle tentative prévue.")
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auth (requise pour le payout uniquement)
    // ─────────────────────────────────────────────────────────────────────────

    private String authenticate() {
        String url = props.getBaseUrl() + AUTH_PATH
                + "?i_space_key=" + props.getISpaceKey()
                + "&app_space_key=" + props.getAppSpaceKey();
        try {
            String raw = restClient.post()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String status = root.path("status").asText();
            if (!"LOGIN_SUCCESS".equals(status)) {
                throw new BusinessException("Authentification échouée : " + status,
                        HttpStatus.BAD_GATEWAY, "MTN_AUTH_ERROR");
            }
            return root.path("data").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[MTN] Erreur d'authentification : {}", e.getMessage());
            throw new BusinessException("Authentification MTN échouée.", HttpStatus.BAD_GATEWAY, "MTN_AUTH_ERROR");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[\\s\\-()]", "").replaceFirst("^\\+", "");
        if (cleaned.startsWith("0")) cleaned = "237" + cleaned.substring(1);
        if (!cleaned.startsWith("237")) cleaned = "237" + cleaned;
        return cleaned;
    }

    private String mapToGatewayStatus(String rawStatus) {
        if (rawStatus == null) return "PENDING";
        return switch (rawStatus.toUpperCase()) {
            case "SUCCESS"  -> "SUCCESS";
            case "FAILED",
                 "TIMEOUT"  -> "FAILED";
            case "CANCELED" -> "CANCELLED";
            default         -> "PENDING";
        };
    }
}
