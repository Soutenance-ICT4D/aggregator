package com.sharepay.aggregator.modules.account.controller;

import com.sharepay.aggregator.modules.account.dto.request.ForgotPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.LoginRequest;
import com.sharepay.aggregator.modules.account.dto.request.RefreshTokenRequest;
import com.sharepay.aggregator.modules.account.dto.request.RegisterRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResendEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResetPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyResetOtpRequest;
import com.sharepay.aggregator.modules.account.dto.response.AuthInfoResponse;
import com.sharepay.aggregator.modules.account.dto.response.AuthLoginResponse;
import com.sharepay.aggregator.modules.account.dto.response.ResetTokenResponse;
import com.sharepay.aggregator.modules.account.service.AuthService;
import com.sharepay.aggregator.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification")
@io.swagger.v3.oas.annotations.security.SecurityRequirements()
public class AuthController {
    
    private final AuthService authService;

    // 1. Inscription
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscription d'un nouvel utilisateur")
    public ApiResponse<AuthInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthInfoResponse response = authService.register(request);
        return ApiResponse.success("Inscription réussie. Veuillez vérifier votre boîte mail pour le code OTP.", response);
    }

    // 2. Vérification de l'email
    @PostMapping("/verify-email-otp")
    @Operation(summary = "Vérifier l'email via OTP", description = "Vérifie le code OTP envoyé à l'adresse email de l'utilisateur pour activer son compte")
    public ApiResponse<AuthInfoResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthInfoResponse response = authService.verifyEmail(request);
        return ApiResponse.success("Email vérifié avec succès. Votre compte est maintenant actif.", response);
    }
    
    // 3. Renvoyer le code de vérification
    @PostMapping("/resend-verify-email-code")
    @Operation(summary = "Renvoyer le code OTP", description = "Génère et renvoie un nouveau code OTP de vérification à l'adresse email spécifiée")
    public ApiResponse<Void> resendVerifyEmailCode(@Valid @RequestBody ResendEmailRequest request) {
        authService.resendVerificationEmail(request);
        return ApiResponse.success("Un nouveau code de vérification a été envoyé à votre adresse email.", null);
    }

    // 4. Connexion (Login)
    @PostMapping("/login")
    @Operation(summary = "Connexion d'un utilisateur", description = "Authentifie un utilisateur et retourne un access token et un refresh token")
    public ApiResponse<AuthLoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        
        AuthLoginResponse response = authService.login(request, ipAddress, userAgent);
        return ApiResponse.success("Connexion réussie.", response);
    }

    // 5. Renouvellement de token (Refresh)
    @PostMapping("/refresh-token")
    @Operation(summary = "Renouveler le token", description = "Délivre un nouvel Access Token (et Refresh Token) à partir d'un Refresh Token valide")
    public ApiResponse<AuthLoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        
        String ipAddress = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        
        AuthLoginResponse response = authService.refreshToken(request, ipAddress, userAgent);
        return ApiResponse.success("Token renouvelé avec succès.", response);
    }

    // 6. Déconnexion (Logout)
    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Révoque le Refresh Token de la session utilisateur")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success("Déconnexion réussie.", null);
    }

    // 7. Mot de passe oublié (Demande OTP)
    @PostMapping("/forgot-password")
    @Operation(summary = "Mot de passe oublié", description = "Envoie un code OTP par email pour réinitialiser le mot de passe")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("Si cet email existe, un code de réinitialisation a été envoyé.", null);
    }

    // 8. Vérification de l'OTP de réinitialisation
    @PostMapping("/verify-reset-password-otp")
    @Operation(summary = "Vérifier l'OTP de réinitialisation", description = "Valide l'OTP et retourne un jeton court pour autoriser le changement de mot de passe")
    public ApiResponse<ResetTokenResponse> verifyResetPasswordOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        ResetTokenResponse response = authService.verifyResetPasswordOtp(request);
        return ApiResponse.success("Code vérifié. Vous pouvez maintenant définir un nouveau mot de passe.", response);
    }

    // 9. Réinitialisation effective du mot de passe
    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Définit le nouveau mot de passe à l'aide du jeton autorisé")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Mot de passe modifié avec succès. Déconnexion des autres appareils effectuée.", null);
    }
}

