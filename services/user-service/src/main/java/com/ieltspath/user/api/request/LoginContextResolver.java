package com.ieltspath.user.api.request;

import com.ieltspath.user.application.command.LoginContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public final class LoginContextResolver {
    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private LoginContextResolver() {
    }

    /**
     * The Gateway appends the address it saw to {@code X-Forwarded-For}, so the last entry is the one it vouches for;
     * earlier entries are whatever the client sent and are ignored.
     */
    public static LoginContext from(HttpServletRequest request) {
        return new LoginContext(clientAddress(request), request.getHeader(HttpHeaders.USER_AGENT));
    }

    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
