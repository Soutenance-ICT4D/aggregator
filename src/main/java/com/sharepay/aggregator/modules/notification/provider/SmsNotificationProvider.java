package com.sharepay.aggregator.modules.notification.provider;

import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationProvider implements NotificationProvider {

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.SMS;
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("============== SIMULATION SMS ==============");
        log.info("Envoi d'un SMS à : {}", message.getRecipient());
        log.info("Contenu des variables : {}", message.getVariables());
        log.info("========================================");
        
        // TODO: Implémenter la véritable logique d'envoi de SMS (ex: Twilio) ici
    }
}
