package com.sharepay.aggregator.modules.notification.provider;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationProvider implements NotificationProvider {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${app.mail.from}")
    private String senderEmail;

    private final SpringTemplateEngine templateEngine;

    @Override
    public boolean supports(NotificationType type) {
        return type == NotificationType.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        try {
            String templateName = message.getTemplate().name().toLowerCase().replace("_", "-");
            Context context = new Context();
            if (message.getVariables() != null) {
                context.setVariables(message.getVariables());
            }
            String htmlContent = templateEngine.process("email/" + templateName, context);

            Mail mail = new Mail(
                    new Email(senderEmail),
                    message.getSubject(),
                    new Email(message.getRecipient()),
                    new Content("text/html", htmlContent)
            );

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = new SendGrid(sendGridApiKey).api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email envoyé avec succès à {} (status {})", message.getRecipient(), response.getStatusCode());
            } else {
                log.error("Erreur SendGrid {} pour {} : {}", response.getStatusCode(), message.getRecipient(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email à {}", message.getRecipient(), e);
        }
    }
}
