package com.sharepay.aggregator.modules.notification.provider;

import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@sharepay.com}")
    private String senderEmail;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(message.getRecipient());
            helper.setSubject(message.getSubject());
            
            Context context = new Context();
            if (message.getVariables() != null) {
                context.setVariables(message.getVariables());
            }

            String templateName = message.getTemplate().name().toLowerCase().replace("_", "-");
            String htmlContent = templateEngine.process("email/" + templateName, context);
            
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Email envoyé avec succès à {}", message.getRecipient());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {}", message.getRecipient(), e);
        }
    }
}


