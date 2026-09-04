package com.groww.hackathon.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserIdentityResolver {

    private static final String COOKIE_NAME = "GROWW_UID";
    private static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

    // NOTE for README: this is a per-browser anonymous id, not a true cross-device
    // identity — a real system swaps this for an authenticated user id, and nothing
    // else in the architecture changes, since everything is already keyed on userId.
    public String resolve(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (COOKIE_NAME.equals(c.getName())) return c.getValue();
            }
        }
        String newId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(COOKIE_NAME, newId);
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        response.addCookie(cookie);
        return newId;
    }
}