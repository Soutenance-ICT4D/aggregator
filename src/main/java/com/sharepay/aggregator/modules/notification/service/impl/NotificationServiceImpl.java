package com.sharepay.aggregator.modules.notification.service.impl;

import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import com.sharepay.aggregator.modules.notification.provider.NotificationProvider;
import com.sharepay.aggregator.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationProvider> notificationProviders;

    @Async
    @Override
    public void send(NotificationMessage message) {

        log.info("Routage de la notification de type : {}", message.getType());

        for (NotificationProvider provider : notificationProviders) {
            if (provider.supports(message.getType())) {
                provider.send(message);
                return;
            }
        }

        log.error("Aucun fournisseur de notification trouvé pour le type : {}", message.getType());
        // Vous pouvez lancer une exception ici si nécessaire, ex: throw new UnsupportedOperationException()
    }
}

