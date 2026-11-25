package com.ieltspath.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.apigateway.error.GatewayErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@RequiredArgsConstructor
public class TokenRevocationWebFilter implements WebFilter {
    private final RevocationChecker checker;
    private final RevocationProperties properties;
    private final ObjectMapper mapper;
    private final MeterRegistry meters;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> checker.isRevoked(authentication.getToken())
                        .flatMap(revoked -> revoked
                                ? writeError(exchange, HttpStatus.UNAUTHORIZED, "SESSION_REVOKED",
                                "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.")
                                : chain.filter(exchange))
                        .onErrorResume(error -> {
                            log.warn("Không thể kiểm tra trạng thái thu hồi access token");
                            return properties.failClosed()
                                    ? writeError(exchange, HttpStatus.SERVICE_UNAVAILABLE, "AUTH_UNAVAILABLE",
                                    "Hệ thống tạm thời không xác minh được phiên đăng nhập. Vui lòng thử lại sau.")
                                    : chain.filter(exchange);
                        }))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        GatewayErrorResponse response = new GatewayErrorResponse(LocalDateTime.now(), status.value(),
                status == HttpStatus.UNAUTHORIZED ? "Chưa xác thực" : "Dịch vụ tạm thời không khả dụng",
                message, exchange.getRequest().getPath().value(), null, code);
        try {
            byte[] json = mapper.writeValueAsBytes(response);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(json)));
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}
