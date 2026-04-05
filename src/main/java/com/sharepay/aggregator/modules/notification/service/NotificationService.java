package com.sharepay.aggregator.modules.notification.service;

import com.sharepay.aggregator.modules.notification.dto.NotificationMessage;

public interface NotificationService {
    void send(NotificationMessage message);
}
