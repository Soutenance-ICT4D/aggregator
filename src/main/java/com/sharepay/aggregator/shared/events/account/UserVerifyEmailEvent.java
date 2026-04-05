package com.sharepay.aggregator.shared.events.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserVerifyEmailEvent {
    private final String email;
    private final String otp;
}
