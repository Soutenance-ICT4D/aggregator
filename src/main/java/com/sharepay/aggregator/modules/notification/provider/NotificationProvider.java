package com.sharepay.aggregator.modules.notification.provider;

import com.sharepay.aggregator.modules.notification.constant.NotificationType;
import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;

public interface NotificationProvider {
    boolean supports(NotificationType type);
    void send(NotificationMessage message);
}
