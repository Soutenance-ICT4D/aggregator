package com.sharepay.aggregator.modules.account.service.impl;

import com.sharepay.aggregator.modules.account.dto.request.LoginRequest;
import com.sharepay.aggregator.modules.account.dto.request.RegisterRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResendEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyEmailRequest;
import com.sharepay.aggregator.modules.account.dto.request.ForgotPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.request.VerifyResetOtpRequest;
import com.sharepay.aggregator.modules.account.dto.request.ResetPasswordRequest;
import com.sharepay.aggregator.modules.account.dto.response.AuthInfoResponse;
import com.sharepay.aggregator.modules.account.dto.response.AuthLoginResponse;
import com.sharepay.aggregator.modules.account.dto.response.ResetTokenResponse;
import com.sharepay.aggregator.modules.account.model.RefreshToken;
import com.sharepay.aggregator.modules.account.model.OtpCode;
import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.account.repository.OtpCodeRepository;
import com.sharepay.aggregator.modules.account.repository.RefreshTokenRepository;
import com.sharepay.aggregator.modules.account.repository.UserRepository;
import com.sharepay.aggregator.modules.account.service.AuthService;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.KycLevel;
import com.sharepay.aggregator.shared.constant.OtpPurpose;
import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.events.account.UserVerifyEmailEvent;
import com.sharepay.aggregator.shared.events.account.UserWelcomeEvent;
import com.sharepay.aggregator.shared.events.account.UserForgotPasswordEvent;
import com.sharepay.aggregator.shared.exception.BusinessException;
import com.sharepay.aggregator.shared.util.HashUtil;
import com.sharepay.aggregator.shared.util.OtpUtil;
import com.sharepay.aggregator.shared.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import java.security.MessageDigest;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpirationMs;

    private static final int OTP_EXPIRY_MINUTES = 15;

    @Override
    @Transactional
    public AuthInfoResponse register(RegisterRequest request) {
        // Vérifier si email déjà utilisé
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (existingUser.isEmailVerified()) {
                throw new BusinessException(
                    "Cet email est déjà utilisé", 
                    HttpStatus.CONFLICT, 
                    "EMAIL_ALREADY_EXISTS"
                );
            }
            // Si non vérifié, on le remplace
            log.info("Replacing unverified account for email: {}", request.getEmail());
            otpCodeRepository.deleteByUser(existingUser);
            userRepository.delete(existingUser);
            userRepository.flush();
        });

        // Création de l'utilisateur
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .country(request.getCountry())
                .status(AccountStatus.ACTIVE)
                .role(Role.MERCHANT)
                .kycLevel(KycLevel.NONE)
                .build();
        
        user = userRepository.save(user);

        // Génération OTP sécurisée
        String otp = OtpUtil.generateSecureOtp(6);
        
        // Stockage OTP hashé
        OtpCode otpCode = OtpCode.builder()
                .user(user)
                .codeHash(HashUtil.sha256(otp))
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        
        otpCodeRepository.save(otpCode);

        // Déclencher l'événement de vérification (découplage)
        eventPublisher.publishEvent(new UserVerifyEmailEvent(user.getEmail(), otp));
        log.info("UserVerifyEmailEvent published for: {}", user.getEmail());

        return buildAuthResponse(user);
    }
    
    @Override
    @Transactional
    public AuthInfoResponse verifyEmail(VerifyEmailRequest request) {
        // 1. Récupérer l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouvé",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));

        // 2. Vérifier si déjà actif
        if (user.isEmailVerified()) {
            throw new BusinessException(
                    "Ce compte est déjà vérifié",
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_ALREADY_VERIFIED"
            );
        }

        // 3. Récupérer le dernier OTP pour cet utilisateur
        OtpCode otpCode = otpCodeRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BusinessException(
                        "Aucun code de vérification trouvé",
                        HttpStatus.NOT_FOUND,
                        "OTP_NOT_FOUND"
                ));

        // 4. Vérifier l'expiration (Convertir ExpiresAt OffsetDateTime en LocalDateTime UTC)
        // Note: isBefore fonctionne entre OffsetDateTime. Mais ici c'est stocké en OffsetDateTime.
        if (otpCode.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(
                    "Le code de vérification a expiré",
                    HttpStatus.BAD_REQUEST,
                    "OTP_EXPIRED"
            );
        }

        // 5. Vérifier la validité du code
        if (!MessageDigest.isEqual(
                HashUtil.sha256(request.getOtp()).getBytes(),
                otpCode.getCodeHash().getBytes())) {
            throw new BusinessException(
                    "Code de vérification invalide",
                    HttpStatus.BAD_REQUEST,
                    "OTP_INVALID"
            );
        }

        // 6. Mettre à jour emailVerified (le statut reste PENDING_VERIFICATION jusqu'à validation admin)
        user.setEmailVerified(true);
        userRepository.save(user);

        // 7. Nettoyer l'OTP
        otpCodeRepository.delete(otpCode);

        // Déclencher l'événement de bienvenue
        eventPublisher.publishEvent(new UserWelcomeEvent(this, user.getId(), user.getEmail(), user.getFullName()));
        log.info("UserWelcomeEvent published for: {}", user.getEmail());

        // 8. Retourner les informations
        return buildAuthResponse(user);
    }
    @Override
    @Transactional
    public void resendVerificationEmail(ResendEmailRequest request) {
        // 1. Récupérer l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouvé",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));

        // 2. Vérifier si déjà actif
        if (user.isEmailVerified()) {
            throw new BusinessException(
                    "Ce compte est déjà vérifié",
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_ALREADY_VERIFIED"
            );
        }

        // 3. Supprimer tous les anciens codes OTP d'email pour éviter la confusion
        otpCodeRepository.deleteByUser(user);
        otpCodeRepository.flush();

        // 4. Génération du nouvel OTP sécurisé
        String otp = OtpUtil.generateSecureOtp(6);

        // 5. Stockage OTP hashé
        OtpCode otpCode = OtpCode.builder()
                .user(user)
                .codeHash(HashUtil.sha256(otp))
                .purpose(OtpPurpose.EMAIL_VERIFICATION)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpCodeRepository.save(otpCode);

        // 6. Déclencher l'événement de vérification
        eventPublisher.publishEvent(new UserVerifyEmailEvent(user.getEmail(), otp));
        log.info("UserVerifyEmailEvent published for resend to: {}", user.getEmail());
    }

    @Override
    @Transactional
    public AuthLoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // 1. Trouver l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "Identifiants invalides",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "Identifiants invalides",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }

        // 3. Vérifier si l'email est vérifié
        if (!user.isEmailVerified()) {
            throw new BusinessException(
                    "Veuillez vérifier votre adresse email avant de vous connecter.",
                    HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED"
            );
        }

        // Vérifier si le compte est interdit d'accès (les PENDING_VERIFICATION sont autorisés)
        if (user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException(
                    "Compte suspendu ou supprimé. Veuillez contacter le support.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE"
            );
        }

        // 4. Générer l'Access Token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        // 5. Générer le Refresh Token (Chaine pseudo-aléatoire sécurisée)
        String rawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String hashedRefreshToken = HashUtil.sha256(rawRefreshToken);

        // 6. Sauvegarder le Refresh Token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashedRefreshToken)
                .familyId(UUID.randomUUID()) // Nouvelle session = nouvelle famille
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plus(refreshTokenExpirationMs, ChronoUnit.MILLIS)) // Conversion ms -> ns
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(refreshToken);

        // 7. Construire la réponse
        return AuthLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken) // On renvoie la valeur en clair (jamais enregistrée telle quelle)
                .user(buildAuthResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthLoginResponse refreshToken(com.sharepay.aggregator.modules.account.dto.request.RefreshTokenRequest request, String ipAddress, String userAgent) {
        String hashedToken = HashUtil.sha256(request.getRefreshToken());

        RefreshToken currentRefreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BusinessException(
                        "Refresh token invalide",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_REFRESH_TOKEN"
                ));

        if (currentRefreshToken.isRevoked()) {
            // Détection de vol de token (Token Theft) : on révoque toute la famille
            log.warn("Token theft detected! Revoking all tokens for family: {}", currentRefreshToken.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(currentRefreshToken.getFamilyId());
            throw new BusinessException(
                    "Session compromise détectée. Veuillez vous reconnecter.",
                    HttpStatus.UNAUTHORIZED,
                    "COMPROMISED_SESSION"
            );
        }

        if (currentRefreshToken.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(
                    "La session a expiré",
                    HttpStatus.UNAUTHORIZED,
                    "EXPIRED_SESSION"
            );
        }

        // Révocation de l'ancien token
        currentRefreshToken.setRevoked(true);
        refreshTokenRepository.save(currentRefreshToken);

        User user = currentRefreshToken.getUser();

        if (user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.DELETED) {
            throw new BusinessException(
                    "Compte suspendu ou supprimé. Impossible de rafraîchir la session.",
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE"
            );
        }

        // Génération de l'Access Token
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        // Génération du nouveau Refresh Token
        String newRawRefreshToken = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        String newHashedRefreshToken = HashUtil.sha256(newRawRefreshToken);

        RefreshToken newRefreshTokenObj = RefreshToken.builder()
                .user(user)
                .tokenHash(newHashedRefreshToken)
                .familyId(currentRefreshToken.getFamilyId()) // Même famille
                .isRevoked(false)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plus(refreshTokenExpirationMs, ChronoUnit.MILLIS))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(newRefreshTokenObj);

        return AuthLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawRefreshToken)
                .user(buildAuthResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(com.sharepay.aggregator.modules.account.dto.request.RefreshTokenRequest request) {
        String hashedToken = HashUtil.sha256(request.getRefreshToken());

        refreshTokenRepository.findByTokenHash(hashedToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Session logout: token revoked for user {}", token.getUser().getEmail());
        });
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Sortie silencieuse si l'email n'existe pas — évite l'énumération d'emails
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            log.debug("Forgot password requested for unknown email: {}", request.getEmail());
            return;
        }

        // Supprimer les anciens codes reset
        otpCodeRepository.deleteByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET);
        otpCodeRepository.flush();

        String otp = OtpUtil.generateSecureOtp(6);

        OtpCode otpCode = OtpCode.builder()
                .user(user)
                .codeHash(HashUtil.sha256(otp))
                .purpose(OtpPurpose.PASSWORD_RESET)
                .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpCodeRepository.save(otpCode);

        eventPublisher.publishEvent(new UserForgotPasswordEvent(this, user.getEmail(), otp));
        log.info("UserForgotPasswordEvent published for: {}", user.getEmail());
    }

    @Override
    @Transactional
    public ResetTokenResponse verifyResetPasswordOtp(VerifyResetOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "Utilisateur non trouvé",
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND"
                ));

        OtpCode otpCode = otpCodeRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException(
                        "Aucun code de réinitialisation trouvé",
                        HttpStatus.NOT_FOUND,
                        "OTP_NOT_FOUND"
                ));

        if (otpCode.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(
                    "Le code de réinitialisation a expiré",
                    HttpStatus.BAD_REQUEST,
                    "OTP_EXPIRED"
            );
        }

        if (!MessageDigest.isEqual(
                HashUtil.sha256(request.getOtp()).getBytes(),
                otpCode.getCodeHash().getBytes())) {
            throw new BusinessException(
                    "Code de réinitialisation invalide",
                    HttpStatus.BAD_REQUEST,
                    "OTP_INVALID"
            );
        }

        // Nettoyer l'OTP après succès
        otpCodeRepository.delete(otpCode);

        // Générer le jeton court pour reset password (5 minutes)
        String resetToken = jwtService.generatePasswordResetToken(user.getId());
        
        return ResetTokenResponse.builder()
                .resetToken(resetToken)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        try {
            Jwt jwt = jwtService.validateToken(request.getResetToken());
            
            if (!jwtService.isPasswordResetToken(jwt)) {
                throw new BusinessException(
                        "Jeton invalide",
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_TOKEN_TYPE"
                );
            }
            
            UUID accountId = jwtService.extractAccountId(jwt);
            
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new BusinessException(
                            "Utilisateur non trouvé",
                            HttpStatus.NOT_FOUND,
                            "USER_NOT_FOUND"
                    ));
            
            // Mise à jour mot de passe
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            
            // Révocation de toutes les sessions existantes (sécurité forte)
            refreshTokenRepository.findByUser(user).forEach(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
            log.info("Password reset successful and all sessions revoked for user: {}", user.getEmail());
            
        } catch (JwtException e) {
            throw new BusinessException(
                    "Le jeton de réinitialisation est invalide ou expiré",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_RESET_TOKEN"
            );
        }
    }

    private AuthInfoResponse buildAuthResponse(User user) {
        return AuthInfoResponse.builder()
                .accountId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .kycLevel(user.getKycLevel())
                .build();
    }
}
