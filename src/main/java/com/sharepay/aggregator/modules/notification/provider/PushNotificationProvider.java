package com.sharepay.aggregator.modules.notification.provider;

import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PushNotificationProvider implements NotificationProvider {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.PUSH;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("============== SIMULATION PUSH / IN-APP ==============");
        log.info("Envoi d'une notification push à : {}", message.getRecipient());
        log.info("Données : {}", message.getVariables());
        log.info("======================================================");
        
        // TODO: Implémenter la véritable logique d'envoi push (ex: Firebase Cloud Messaging, ou WebSocket) ici
    }
}
