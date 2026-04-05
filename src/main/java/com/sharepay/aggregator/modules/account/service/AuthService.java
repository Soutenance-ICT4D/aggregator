package com.sharepay.aggregator.modules.account.service;

import com.sharepay.aggregator.modules.account.dto.request.RegisterRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResendEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.LoginRequest;
import com.sharepay.aggregator.modules.account.dto.request.RefreshTokenRequest;
import com.sharepay.aggregator.modules.account.dto.request.ForgotPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyResetOtpRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResetPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.response.AuthInfoResponse;
import com.sharepay.aggregator.modules.account.dto.response.AuthLoginResponse;
import com.sharepay.aggregator.modules.account.dto.response.ResetTokenResponse;

public interface AuthService {
    AuthInfoResponse register(RegisterRequest request);
    AuthInfoResponse verifyEmail(VerifyEmailRequest request);
    void resendVerificationEmail(ResendEmailRequest request);
    AuthLoginResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthLoginResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent);
    void logout(RefreshTokenRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    ResetTokenResponse verifyResetPasswordOtp(VerifyResetOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
}

