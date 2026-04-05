package com.sharepay.aggregator.shared.security.jwt;

import com.sharepay.aggregator.shared.constant.Role;
import com.sharepay.aggregator.shared.security.AuthentificatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

// Filtre d'authentification JWT.
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String rawToken = authHeader.substring(7);
            Jwt jwt = jwtService.validateToken(rawToken);

            // Accepter uniquement les access tokens (pas les reset tokens)
            if (!jwtService.isAccessToken(jwt)) {
                log.warn("Token de type non-access rejeté : {}", jwt.getClaimAsString("type"));
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
                return;
            }

            UUID accountId = jwtService.extractAccountId(jwt);
            String email   = jwtService.extractEmail(jwt);
            Role role      = jwtService.extractRole(jwt);

            AuthentificatedUser principal = AuthentificatedUser.builder()
                    .accountId(accountId)
                    .email(email)
                    .role(role)
                    .build();

            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.name())
            );

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("JWT valide – compte : {} rôle : {}", accountId, role);

        } catch (JwtValidationException e) {
            boolean isExpired = e.getErrors().stream()
                    .anyMatch(err -> err.getDescription() != null && err.getDescription().contains("expired"));
            log.warn("JWT {} : {}", isExpired ? "expiré" : "invalide", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    isExpired ? "TOKEN_EXPIRED" : "INVALID_TOKEN");
            return;
        } catch (JwtException e) {
            log.warn("JWT invalide : {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la validation JWT : {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR_OCCURRED");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/health")
                || path.equals("/logo_sharepay_svg.svg");
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + code + "\",\"data\":null}"
        );
    }
}
