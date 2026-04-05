package com.sharepay.aggregator.modules.notification.dto;

import com.sharepay.aggregator.modules.notification.constant.EmailTemplate;
import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String recipient;
    private NotificationType type;
    private EmailTemplate template;
    private String subject;
    private Map<String, Object> variables;
}

