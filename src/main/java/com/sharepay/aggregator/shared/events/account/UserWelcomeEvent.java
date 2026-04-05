package com.sharepay.aggregator.shared.events.account;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserWelcomeEvent extends ApplicationEvent {
    
    private final String email;
    private final String fullName;

    public UserWelcomeEvent(Object source, String email, String fullName) {
        super(source);
        this.email = email;
        this.fullName = fullName;
    }
}
