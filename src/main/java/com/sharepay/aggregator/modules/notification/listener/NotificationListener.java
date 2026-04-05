package com.sharepay.aggregator.modules.notification.listener;

import com.sharepay.aggregator.modules.notification.constant.EmailTemplate;
import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import com.sharepay.aggregator.modules.notification.service.NotificationService;
import com.sharepay.aggregator.shared.events.account.UserVerifyEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleUserVerifyEmailEvent(UserVerifyEmailEvent event) {
        log.info("Received UserVerifyEmailEvent for email: {}", event.getEmail());
        
        notificationService.send(NotificationMessage.builder()
                .recipient(event.getEmail())
                .type(NotificationType.EMAIL)
                .template(EmailTemplate.VERIFY_EMAIL)
                .subject("Vérifiez votre compte Sharepay")
                .variables(Map.of("otp", event.getOtp()))
                .build());
    }
    @EventListener
    public void handleUserWelcomeEvent(com.sharepay.aggregator.shared.events.account.UserWelcomeEvent event) {
        log.info("Received UserWelcomeEvent for email: {}", event.getEmail());
        
        notificationService.send(NotificationMessage.builder()
                .recipient(event.getEmail())
                .type(NotificationType.EMAIL)
                .template(EmailTemplate.WELCOME)
                .subject("Bienvenue sur Sharepay !")
                .variables(Map.of("fullName", event.getFullName()))
                .build());
    }

    @EventListener
    public void handleUserForgotPasswordEvent(com.sharepay.aggregator.shared.events.account.UserForgotPasswordEvent event) {
        log.info("Received UserForgotPasswordEvent for email: {}", event.getEmail());
        
        notificationService.send(NotificationMessage.builder()
                .recipient(event.getEmail())
                .type(NotificationType.EMAIL)
                .template(EmailTemplate.RESET_PASSWORD)
                .subject("Réinitialisation de votre mot de passe")
                .variables(Map.of("otp", event.getOtp()))
                .build());
    }
}


