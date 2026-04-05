package com.sharepay.aggregator.shared.util;

import java.security.SecureRandom;

public class OtpUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateSecureOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }
}
