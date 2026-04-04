package com.sharepay.aggregator.shared.security.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.sharepay.aggregator.shared.constant.AccountStatus;
import com.sharepay.aggregator.shared.constant.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

// Service de gestion des JSON Web Tokens (JWT).
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:2592000000}")
    private long refreshTokenExpiration;

    private NimbusJwtEncoder encoder;
    private NimbusJwtDecoder decoder;

    @PostConstruct
    private void init() {
        byte[] keyBytes = this.secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                .build();

        JWKSet jwkSet = new JWKSet(jwk);
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(jwkSet));
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Génération
    public String generateAccessToken(UUID accountId, String email, Role role, AccountStatus status) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(this.accessTokenExpiration);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountId.toString())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("email", email)
                .claim("role", role.name())
                .claim("status", status.name())
                .claim("type", "access")
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Génère un token de réinitialisation de mot de passe (5 min).
    public String generatePasswordResetToken(UUID accountId) {
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(300); // 5 minutes

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(accountId.toString())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("type", "password_reset")
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Validation & extraction
    public Jwt validateToken(String token) {
        return decoder.decode(token);
    }

    public boolean isAccessToken(Jwt jwt) {
        return "access".equals(jwt.getClaimAsString("type"));
    }

    public boolean isPasswordResetToken(Jwt jwt) {
        return "password_reset".equals(jwt.getClaimAsString("type"));
    }

    public UUID extractAccountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public Role extractRole(Jwt jwt) {
        String roleStr = jwt.getClaimAsString("role");
        return roleStr != null ? Role.valueOf(roleStr) : null;
    }

    public String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public String extractStatus(Jwt jwt) {
        return jwt.getClaimAsString("status");
    }
}
