package com.sharepay.aggregator.shared.security;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Filtre de limitation de débit (Rate Limiting) basé sur une fenêtre glissante in-memory.
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Règles par endpoint : path -> [maxRequests, windowSeconds]
    private static final Map<String, int[]> RULES = Map.of(
        "/api/v1/auth/login",                      new int[]{10, 300},
        "/api/v1/auth/register",                   new int[]{5,  900},
        "/api/v1/auth/forgot-password",            new int[]{3,  900},
        "/api/v1/auth/verify-email-otp",           new int[]{5,  300},
        "/api/v1/auth/verify-reset-password-otp",  new int[]{5,  300},
        "/api/v1/auth/resend-verify-email-code",   new int[]{3,  900}
    );

    // Durée maximale de fenêtre (en ms) — utilisée pour le nettoyage périodique.
    private static final long MAX_WINDOW_MS = 900_000L;

    // Fenêtre glissante : "ip:path" -> file de timestamps (ms).
    private final ConcurrentHashMap<String, Deque<Long>> windowStore = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-cleaner");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void startCleaner() {
        cleaner.scheduleAtFixedRate(this::evictExpiredEntries, 5, 5, TimeUnit.MINUTES);
        log.info("RateLimitingFilter initialized with {} rules", RULES.size());
    }

    @PreDestroy
    public void stopCleaner() {
        cleaner.shutdownNow();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int[] rule = RULES.get(path);

        // Pas de règle pour ce chemin -> on laisse passer
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int maxRequests   = rule[0];
        long windowMs     = rule[1] * 1000L;
        String clientIp   = resolveClientIp(request);
        String key        = clientIp + ":" + path;

        int remaining = tryConsume(key, maxRequests, windowMs);

        if (remaining < 0) {
            log.warn("Rate limit exceeded — IP={} path={}", clientIp, path);
            rejectRequest(response, maxRequests, rule[1]);
            return;
        }

        response.setHeader("X-RateLimit-Limit",     String.valueOf(maxRequests));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    // Fenêtre glissante : retire les entrées expirées, vérifie le quota puis enregistre
    // la requête courante.
    // @return le nombre de requêtes restantes, ou -1 si le quota est dépassé.
    private int tryConsume(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = windowStore.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            long cutoff = now - windowMs;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return -1;
            }
            timestamps.addLast(now);
            return maxRequests - timestamps.size();
        }
    }

    // Supprime les entrées dont la dernière requête remonte à plus de MAX_WINDOW_MS.
    // Appelé toutes les 5 minutes pour éviter les fuites mémoire.
    private void evictExpiredEntries() {
        long cutoff = System.currentTimeMillis() - MAX_WINDOW_MS;
        int before = windowStore.size();
        windowStore.entrySet().removeIf(entry -> {
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                return timestamps.isEmpty() || timestamps.peekLast() < cutoff;
            }
        });
        int removed = before - windowStore.size();
        if (removed > 0) {
            log.debug("Rate limit store eviction: {} stale entries removed", removed);
        }
    }

    // Résout l'IP réelle du client en tenant compte des proxies inverses.
    // Priorité : X-Forwarded-For > X-Real-IP > RemoteAddr.
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    // Renvoie une réponse HTTP 429 avec un corps JSON cohérent avec ApiResponse.
    private void rejectRequest(HttpServletResponse response, int maxRequests, int windowSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
        response.setHeader("Retry-After",        String.valueOf(windowSeconds));

        String json = String.format(
            "{\"success\":false,\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Trop de tentatives. Veuillez r\u00e9essayer dans %d secondes.\",\"data\":null}",
            windowSeconds
        );
        response.getWriter().write(json);
    }
}
