package com.sharepay.aggregator.shared.config.swagger;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Base64;

// Filtre d'authentification pour l'accès à la documentation Swagger des développeurs externes.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ExternalDocsAuthFilter implements Filter {

    private static final String EXTERNAL_DOCS_PATH = "/swagger-ui/devs/sharepay-docs";
    private static final String LOGIN_PATH = "/swagger-ui/devs/sharepay-docs/login";
    private static final String COOKIE_NAME = "sharepay_external_session";

    private final String validSessionToken;

    public ExternalDocsAuthFilter() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        this.validSessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        if (!path.equals(EXTERNAL_DOCS_PATH) &&
                !path.equals(EXTERNAL_DOCS_PATH + "/") &&
                !path.startsWith(LOGIN_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.startsWith(LOGIN_PATH)) {
            handleLoginSubmit(httpRequest, httpResponse);
            return;
        }

        if ("GET".equalsIgnoreCase(method) &&
                path.startsWith(LOGIN_PATH) &&
                "true".equals(httpRequest.getParameter("logout"))) {
            handleLogout(httpResponse);
            return;
        }

        if ("GET".equalsIgnoreCase(method) && path.startsWith(LOGIN_PATH)) {
            serveLoginPage(httpResponse, false);
            return;
        }

        if (isAuthenticated(httpRequest)) {
            serveExternalDocsPage(httpResponse);
        } else {
            httpResponse.sendRedirect(LOGIN_PATH);
        }
    }

    private void handleLoginSubmit(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (isValidCredentials(email, password)) {
            Cookie sessionCookie = new Cookie(COOKIE_NAME, validSessionToken);
            sessionCookie.setPath(EXTERNAL_DOCS_PATH);
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(false);
            sessionCookie.setMaxAge(60 * 60 * 24); // 24 heures
            response.addCookie(sessionCookie);
            response.sendRedirect(EXTERNAL_DOCS_PATH);
        } else {
            serveLoginPage(response, true);
        }
    }

    private void handleLogout(HttpServletResponse response) throws IOException {
        Cookie sessionCookie = new Cookie(COOKIE_NAME, "");
        sessionCookie.setPath(EXTERNAL_DOCS_PATH);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(0);
        response.addCookie(sessionCookie);
        response.sendRedirect(LOGIN_PATH);
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) &&
                    validSessionToken.equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validation temporaire des identifiants (Email/Password).
     * TODO: Injecter AuthService ou AccountService pour validation en base de données.
     */
    private boolean isValidCredentials(String email, String password) {
        if (email == null || password == null) return false;
        // Bouchon temporaire
        return email.equals("partner@sharepay.com") && password.equals("password");
    }

    private void serveLoginPage(HttpServletResponse response, boolean withError)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(withError ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_OK);

        String errorBlock = withError ? """
                <div class="error-msg">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/>
                        <line x1="12" y1="16" x2="12.01" y2="16"/>
                    </svg>
                    Identifiants incorrects. Veuillez réessayer.
                </div>
                """ : "";

        PrintWriter out = response.getWriter();
        out.println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Sharepay Developer Portal - Login</title>
              <link rel="icon" type="image/svg+xml" href="/logo_sharepay_svg.svg"/>
              <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet"/>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    min-height: 100vh;
                    background: #ffffff;
                    font-family: 'Inter', sans-serif;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    overflow: hidden;
                }
                body::before {
                    content: '';
                    position: fixed;
                    inset: 0;
                    background:
                        radial-gradient(ellipse at 20% 20%, rgba(10,10,35,0.08) 0%, transparent 60%),
                        radial-gradient(ellipse at 80% 80%, rgba(9,136,101,0.06) 0%, transparent 60%);
                    z-index: 0;
                    animation: gradientShift 10s ease infinite;
                }
                @keyframes gradientShift {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.8; }
                }
                .card {
                    position: relative;
                    z-index: 1;
                    background: #ffffff;
                    border: 1px solid #e2e8f0;
                    border-radius: 20px;
                    padding: 48px 44px;
                    width: 520px;
                    box-shadow: 0 24px 80px rgba(0,0,0,0.08), 0 0 1px rgba(0,0,0,0.1);
                }
                .logo-wrap {
                    display: flex;
                    justify-content: center;
                    margin-bottom: 8px;
                }
                .logo-wrap img { height: 52px; }
                .badge {
                    text-align: center;
                    font-size: 11px;
                    font-weight: 600;
                    letter-spacing: 0.12em;
                    color: #098865;
                    margin-bottom: 8px;
                }
                h1 {
                    font-size: 24px;
                    font-weight: 700;
                    color: #0f172a;
                    text-align: center;
                    margin-bottom: 8px;
                }
                .subtitle {
                    text-align: center;
                    font-size: 14px;
                    color: #64748b;
                    margin-bottom: 24px;
                    line-height: 1.5;
                }
                .info-box {
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                    background: #f0fdf4;
                    border: 1px solid #bbf7d0;
                    border-radius: 12px;
                    padding: 16px;
                    margin-bottom: 32px;
                    color: #166534;
                    font-size: 13.5px;
                    line-height: 1.5;
                    box-shadow: 0 2px 8px rgba(22, 101, 52, 0.05);
                }
                .info-box svg {
                    flex-shrink: 0;
                    color: #15803d;
                    margin-top: 2px;
                }
                .info-box strong { color: #14532d; font-weight: 700; }
                label {
                    display: block;
                    font-size: 13px;
                    font-weight: 600;
                    color: #475569;
                    letter-spacing: 0.02em;
                    margin-bottom: 8px;
                }
                .field { margin-bottom: 20px; }
                input[type=email], input[type=password] {
                    width: 100%;
                    padding: 14px 16px;
                    background: #f8fafc;
                    border: 1.5px solid #cbd5e1;
                    border-radius: 10px;
                    color: #0f172a;
                    font-family: inherit;
                    font-size: 15px;
                    outline: none;
                    transition: all 0.2s;
                }
                input:focus {
                    background: #ffffff;
                    border-color: #098865;
                    box-shadow: 0 0 0 3px rgba(9,136,101,0.12);
                }
                button[type=submit] {
                    width: 100%;
                    padding: 15px;
                    background: linear-gradient(135deg, #098865 0%, #076d51 100%);
                    color: #fff;
                    font-family: inherit;
                    font-size: 15px;
                    font-weight: 600;
                    border: none;
                    border-radius: 10px;
                    cursor: pointer;
                    margin-top: 8px;
                    transition: all 0.2s;
                    box-shadow: 0 2px 8px rgba(9,136,101,0.25);
                }
                button[type=submit]:hover { 
                    transform: translateY(-1px);
                    box-shadow: 0 4px 12px rgba(9,136,101,0.35);
                }
                button[type=submit]:active { transform: translateY(0); }
                .error-msg {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    background: rgba(239,68,68,0.08);
                    border: 1.5px solid rgba(239,68,68,0.2);
                    border-radius: 10px;
                    padding: 12px 16px;
                    color: #dc2626;
                    font-size: 14px;
                    margin-bottom: 24px;
                    animation: shake 0.4s;
                }
                @keyframes shake {
                    0%, 100% { transform: translateX(0); }
                    25% { transform: translateX(-8px); }
                    75% { transform: translateX(8px); }
                }
                .footer-info {
                    text-align: center;
                    margin-top: 28px;
                    padding-top: 24px;
                    border-top: 1px solid #e2e8f0;
                    font-size: 12px;
                    color: #94a3b8;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo-wrap">
                    <img src="/logo_sharepay_svg.svg" alt="Sharepay Logo"/>
                </div>
                <p class="badge">DEVELOPER PORTAL</p>
                <h1>API Documentation</h1>
                <p class="subtitle">Veuillez utiliser vos identifiants <strong>SharePay</strong>.</p>
            """ + errorBlock + """

                <form method="POST" action="/swagger-ui/devs/sharepay-docs/login">
                    <div class="field">
                        <label for="email">Adresse email SharePay</label>
                        <input type="email" id="email" name="email"
                               placeholder="nom@entreprise.com" 
                               autocomplete="email" 
                               required 
                               autofocus/>
                    </div>
                    <div class="field">
                        <label for="password">Mot de passe</label>
                        <input type="password" id="password" name="password"
                               placeholder="••••••••••••" 
                               autocomplete="current-password" 
                               required/>
                    </div>
                    <button type="submit">Accéder à la Documentation →</button>
                </form>

                <div class="footer-info">
                    <strong>Accès strictement réservé</strong> aux partenaires Sharepay.<br/>
                </div>
              </div>
            </body>
            </html>
            """);
    }

    private void serveExternalDocsPage(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter out = response.getWriter();
        out.println("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Sharepay Integration API Documentation</title>
              <link rel="icon" type="image/svg+xml" href="/logo_sharepay_svg.svg"/>
              <link rel="stylesheet" href="/swagger-ui/swagger-ui.css"/>
              <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet"/>
              <style>
                body { margin: 0; background: #fafafa; font-family: 'Inter', sans-serif; }

                /* ── Topbar personnalisée Sharepay ── */
                .swagger-ui .topbar { 
                    background: linear-gradient(135deg, #0a0a23 0%, #1a1a3e 100%);
                    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                }
                .swagger-ui .topbar .topbar-wrapper a.link svg { display: none; }
                .swagger-ui .topbar .topbar-wrapper a.link::before {
                    content: "";
                    display: inline-block;
                    background: url('/logo_sharepay_svg.svg') no-repeat left center / contain;
                    width: 40px;
                    height: 40px;
                    vertical-align: middle;
                }
                
                /* Texte SharePay en vert du thème */
                .swagger-ui .topbar .topbar-wrapper a.link::after {
                    content: "SharePay";
                    color: #10b981;
                    font-family: 'Inter', sans-serif;
                    font-size: 22px;
                    font-weight: 700;
                    margin-left: 12px;
                    vertical-align: middle;
                    letter-spacing: -0.5px;
                }

                /* Badge "External Portal" dans la topbar */
                .swagger-ui .topbar .topbar-wrapper::after {
                    content: "External Portal";
                    color: #fbbf24;
                    font-family: 'Inter', sans-serif;
                    font-size: 11px;
                    font-weight: 700;
                    letter-spacing: 0.1em;
                    text-transform: uppercase;
                    margin-left: 20px;
                    align-self: center;
                    background: rgba(251, 191, 36, 0.15);
                    padding: 4px 12px;
                    border-radius: 6px;
                    border: 1px solid rgba(251, 191, 36, 0.3);
                }

                /* Masquer le sélecteur de groupe (un seul groupe disponible) */
                .swagger-ui .topbar .topbar-wrapper .download-url-wrapper,
                .swagger-ui .topbar .topbar-wrapper form { 
                    display: none !important; 
                }

                /* Logout button */
                .logout-btn {
                    position: fixed;
                    top: 10px;
                    right: 20px;
                    z-index: 9999;
                    background: rgba(255, 255, 255, 0.15);
                    border: 1.5px solid rgba(255, 255, 255, 0.3);
                    border-radius: 8px;
                    color: rgba(255, 255, 255, 0.95);
                    font-family: 'Inter', sans-serif;
                    font-size: 13px;
                    font-weight: 500;
                    padding: 7px 16px;
                    cursor: pointer;
                    transition: all 0.2s;
                    text-decoration: none;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                .logout-btn:hover {
                    background: rgba(255, 255, 255, 0.25);
                    transform: translateY(-1px);
                    color: white;
                }
              </style>
            </head>
            <body>

              <!-- Logout button -->
              <a class="logout-btn" href="/swagger-ui/devs/sharepay-docs/login?logout=true"
                 onclick="return confirm('Voulez-vous vraiment vous déconnecter?');">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                  <polyline points="16 17 21 12 16 7"/>
                  <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
                Déconnexion
              </a>

              <div id="swagger-ui"></div>

              <script src="/swagger-ui/swagger-ui-bundle.js"></script>
              <script src="/swagger-ui/swagger-ui-standalone-preset.js"></script>
              <script>
                window.onload = function() {
                    SwaggerUIBundle({
                        url: "/v3/api-docs/sharepay-aggregator",
                        dom_id: "#swagger-ui",
                        deepLinking: true,
                        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                        layout: "StandaloneLayout",
                        defaultModelsExpandDepth: 1,
                        displayRequestDuration: true,
                        filter: true,
                        tryItOutEnabled: true,
                        persistAuthorization: true
                    });
                };
              </script>
            </body>
            </html>
            """);
    }
}