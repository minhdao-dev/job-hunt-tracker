package com.jobhunt.tracker.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class CookieService {

    @Value("${app.cookie.refresh-token-name}")
    private String refreshTokenName;

    @Value("${app.cookie.refresh-token-path}")
    private String refreshTokenPath;

    @Value("${app.cookie.refresh-token-max-age}")
    private int refreshTokenMaxAge;

    @Value("${app.cookie.secure}")
    private boolean secure;

    public void setRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken) {

        Cookie cookie = new Cookie(refreshTokenName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(refreshTokenPath);
        cookie.setMaxAge(refreshTokenMaxAge);

        response.setHeader("Set-Cookie",
                String.format("%s=%s; HttpOnly; %s SameSite=Strict; Path=%s; Max-Age=%d",
                        refreshTokenName,
                        refreshToken,
                        secure ? "Secure;" : "",
                        refreshTokenPath,
                        refreshTokenMaxAge
                )
        );
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath(refreshTokenPath);
        cookie.setMaxAge(0);   // Xóa cookie
        response.addCookie(cookie);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(c -> refreshTokenName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}