package com.ieltspath.game.infrastructure.client;

import com.ieltspath.game.application.port.CurrentPlayerProfile;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Duration;
import java.util.UUID;

@Component
public class CurrentPlayerProfileClient implements CurrentPlayerProfile {
    private final RestClient client;
    private final ResilientCall resilientCall;
    @Autowired
    public CurrentPlayerProfileClient(RestClient.Builder builder,
            @Value("${game.user.base-url:${USER_SERVICE_URL:http://localhost:8085}}") String baseUrl,
            ResilientCall resilientCall) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = builder.clone().requestFactory(factory).baseUrl(baseUrl).build();
        this.resilientCall = resilientCall;
    }
    CurrentPlayerProfileClient(RestClient.Builder builder, String baseUrl) {
        this(builder, baseUrl, ResilientCall.isolatedDefault());
    }
    public String displayName() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth)
                || !auth.isAuthenticated()) throw new IllegalStateException("Authenticated user required");
        String correlation = UUID.randomUUID().toString();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String supplied = attrs.getRequest().getHeader("X-Correlation-Id");
            if (supplied != null && !supplied.isBlank() && supplied.length() <= 128) correlation = supplied;
        }
        final String correlationId = correlation;
        Profile profile;
        try {
            profile = resilientCall.call("user", CallKind.READ, () -> client.get().uri("/api/users/me")
                    .header("Authorization", "Bearer " + auth.getToken().getTokenValue())
                    .header("X-Correlation-Id", correlationId).retrieve().body(Profile.class));
        } catch (DownstreamUnavailableException unavailable) {
            throw new com.ieltspath.game.domain.exception.SoloGameplayException(503, "USER_PROFILE_UNAVAILABLE",
                    "User profile is temporarily unavailable");
        }
        if (profile == null || profile.id() == null || !auth.getToken().getSubject().equals(profile.id().toString())
                || profile.fullName() == null || profile.fullName().isBlank()) {
            throw new IllegalStateException("User profile is unavailable");
        }
        return profile.fullName();
    }
    private record Profile(UUID id, String fullName) {}
}
