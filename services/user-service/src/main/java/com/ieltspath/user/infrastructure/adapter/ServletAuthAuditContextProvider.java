package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.command.LoginContext;
import com.ieltspath.user.application.port.AuthAuditContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class ServletAuthAuditContextProvider implements AuthAuditContextProvider {
    @Override
    public LoginContext current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return LoginContext.UNKNOWN;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String address = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr()
                : forwarded.substring(forwarded.lastIndexOf(',') + 1).trim();
        return new LoginContext(address, request.getHeader("User-Agent"));
    }
}
