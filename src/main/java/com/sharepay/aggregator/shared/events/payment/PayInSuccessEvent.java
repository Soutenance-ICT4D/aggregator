package com.sharepay.aggregator.shared.events.payment;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class PayInSuccessEvent extends ApplicationEvent {

    private final UUID   userId;
    private final long   netAmount;
    private final String currency;

    public PayInSuccessEvent(Object source, UUID userId, long netAmount, String currency) {
        super(source);
        this.userId    = userId;
        this.netAmount = netAmount;
        this.currency  = currency;
    }
}
