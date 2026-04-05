package com.sharepay.aggregator.shared.events.account;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserForgotPasswordEvent extends ApplicationEvent {
    
    private final String email;
    private final String otp;

    public UserForgotPasswordEvent(Object source, String email, String otp) {
        super(source);
        this.email = email;
        this.otp = otp;
    }
}
