package com.ieltspath.apigateway.ratelimit;

import io.netty.util.NetUtil;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;

/** "user:{sub}" for authenticated callers; otherwise "ip:{address}" using X-Forwarded-For only from trusted proxies. */
public class ClientKeyResolver implements KeyResolver {
    private final List<IpAddressMatcher> trustedProxies;

    public ClientKeyResolver(RateLimitProperties properties) {
        trustedProxies = properties.trustedProxies().stream()
                .filter(proxy -> proxy != null && !proxy.isBlank())
                .map(String::trim)
                .map(IpAddressMatcher::new)
                .toList();
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken().getSubject())
                .filter(subject -> subject != null && !subject.isBlank())
                .map(subject -> "user:" + subject)
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + clientIp(exchange)));
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null) return "unknown";
        String remoteIp = remote.getAddress() == null ? remote.getHostString() : remote.getAddress().getHostAddress();
        if (isTrustedProxy(remoteIp)) {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",", -1);
                for (int index = hops.length - 1; index >= 0; index--) {
                    String hop = hops[index].trim();
                    if (isIpAddress(hop) && !isTrustedProxy(hop)) return hop;
                }
            }
        }
        return remoteIp;
    }

    private boolean isTrustedProxy(String address) {
        return trustedProxies.stream().anyMatch(matcher -> matcher.matches(address));
    }

    private boolean isIpAddress(String address) {
        return NetUtil.isValidIpV4Address(address) || NetUtil.isValidIpV6Address(address);
    }
}
