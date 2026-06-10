package com.sharepay.aggregator.modules.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les connexions SSE actives et publie les changements de statut
 * vers le browser du client en temps réel.
 *
 * Utilisé par :
 *  - Le scheduler (polling Nokash) pour notifier dès qu'un statut change
 *  - Les futurs webhooks opérateurs (même appel, zéro changement frontend)
 */
@Slf4j
@Component
public class CheckoutEventPublisher {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Enregistre une connexion SSE pour un sessionToken donné.
     * Appelé à l'ouverture de la connexion depuis le browser.
     */
    public SseEmitter register(String sessionToken) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // timeout 5 min

        emitter.onCompletion(() -> emitters.remove(sessionToken));
        emitter.onTimeout(()    -> emitters.remove(sessionToken));
        emitter.onError(e       -> emitters.remove(sessionToken));

        emitters.put(sessionToken, emitter);
        log.debug("[SSE] Connexion enregistrée - sessionToken: {}", sessionToken);
        return emitter;
    }

    /**
     * Pousse un événement de statut vers le browser.
     * Ferme automatiquement la connexion si le statut est final.
     *
     * @param sessionToken token de la session de checkout
     * @param status       statut final (SUCCESS, FAILED, CANCELLED)
     * @param message      message d'accompagnement
     */
    public void publish(String sessionToken, String status, String message) {
        if (sessionToken == null) return;

        SseEmitter emitter = emitters.get(sessionToken);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("status-update")
                    .data(Map.of("status", status, "message", message != null ? message : "")));

            log.info("[SSE] Événement publié - sessionToken: {}, status: {}", sessionToken, status);

            // Ferme la connexion dès que le statut est final
            if (!"PENDING".equals(status)) {
                emitter.complete();
                emitters.remove(sessionToken);
            }
        } catch (Exception e) {
            log.warn("[SSE] Impossible d'envoyer l'événement pour {} : {}", sessionToken, e.getMessage());
            emitters.remove(sessionToken);
        }
    }

    public boolean hasSubscriber(String sessionToken) {
        return emitters.containsKey(sessionToken);
    }
}
