package com.ieltspath.apigateway.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.support.StaticApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class ClientRateLimitGatewayFilterFactoryTest {
    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedisRateLimiter limiter;
    private ClientKeyResolver keys;
    private RateLimitProperties properties;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        limiter = mock(RedisRateLimiter.class);
        keys = mock(ClientKeyResolver.class);
        properties = new RateLimitProperties(List.of(), Map.of(
                "auth", new RateLimitProperties.Tier(1, 5),
                "upload", new RateLimitProperties.Tier(1, 5),
                "ai", new RateLimitProperties.Tier(1, 3),
                "default", new RateLimitProperties.Tier(20, 40)));
        meters = new SimpleMeterRegistry();
    }

    @Test
    void rejectsWithRetryAfterWhenBurstExceeded() {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        when(limiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RedisRateLimiter.Response(false, Map.of())));

        MockServerWebExchange exchange = exchange("auth-login", "/api/auth/login");
        filter("auth").filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(meters.counter("rate_limit_rejected_total", "route", "auth-login", "tier", "auth").count())
                .isEqualTo(1);
    }

    @Test
    void sharesLimitAcrossTwoGatewayInstances() throws IOException {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        LettuceConnectionFactory firstConnection = connectionFactory();
        LettuceConnectionFactory secondConnection = connectionFactory();
        RedisRateLimiter firstLimiter = redisLimiter(firstConnection);
        RedisRateLimiter secondLimiter = redisLimiter(secondConnection);
        ClientRateLimitGatewayFilterFactory first = new ClientRateLimitGatewayFilterFactory(
                firstLimiter, keys, properties, meters);
        ClientRateLimitGatewayFilterFactory second = new ClientRateLimitGatewayFilterFactory(
                secondLimiter, keys, properties, meters);

        try (var connection = firstConnection.getConnection()) {
            connection.serverCommands().flushDb();
        }
        MockServerWebExchange lastExchange = null;
        for (int attempt = 0; attempt < 6; attempt++) {
            lastExchange = exchange("auth-login", "/api/auth/login");
            GatewayFilter filter = attempt % 2 == 0 ? first.apply(config("auth")) : second.apply(config("auth"));
            filter.filter(lastExchange, ignored -> Mono.empty()).block();
        }

        assertThat(lastExchange).isNotNull();
        assertThat(lastExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(lastExchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
        firstConnection.destroy();
        secondConnection.destroy();
    }

    @Test
    void keysByUserWhenAuthenticated() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("learner-42")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        ServerWebExchange exchange = exchange("test", "/").mutate()
                .principal(Mono.just(new JwtAuthenticationToken(jwt))).build();

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user:learner-42");
    }

    @Test
    void keysByIpWhenAnonymous() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.7", 1234)));
        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:203.0.113.7");
    }

    @Test
    void ignoresForwardedForFromUntrustedProxy() {
        ClientKeyResolver resolver = new ClientKeyResolver(properties);
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "198.51.100.10")
                .remoteAddress(new java.net.InetSocketAddress("203.0.113.7", 1234)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("ip:203.0.113.7");
    }

    @Test
    void letsRequestsThroughWhenRedisIsDown() {
        when(keys.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.just("ip:203.0.113.9"));
        // RedisRateLimiter returns an allowed response when its Redis script errors.
        when(limiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(new RedisRateLimiter.Response(true, Map.of())));
        MockServerWebExchange exchange = exchange("read-api", "/api/learning/topics");

        filter("default").filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private GatewayFilter filter(String tier) {
        ClientRateLimitGatewayFilterFactory factory = new ClientRateLimitGatewayFilterFactory(
                limiter, keys, properties, meters);
        return factory.apply(config(tier));
    }

    private ClientRateLimitGatewayFilterFactory.Config config(String tier) {
        ClientRateLimitGatewayFilterFactory.Config config = new ClientRateLimitGatewayFilterFactory.Config();
        config.setTier(tier);
        return config;
    }

    private MockServerWebExchange exchange(String routeId, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                Route.async().id(routeId).uri("http://localhost").predicate(ignored -> true).build());
        return exchange;
    }

    private LettuceConnectionFactory connectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        return factory;
    }

    @SuppressWarnings("unchecked")
    private RedisRateLimiter redisLimiter(LettuceConnectionFactory connectionFactory) throws IOException {
        ReactiveStringRedisTemplate redis = new ReactiveStringRedisTemplate(connectionFactory);
        String source = new ClassPathResource("META-INF/scripts/request_rate_limiter.lua")
                .getContentAsString(StandardCharsets.UTF_8);
        RedisScript<List<Long>> script = new DefaultRedisScript<>(source, (Class<List<Long>>) (Class<?>) List.class);
        RedisRateLimiter redisRateLimiter = new RedisRateLimiter(redis, script, (ConfigurationService) null);
        redisRateLimiter.getConfig().put("auth", new RedisRateLimiter.Config()
                .setReplenishRate(1).setBurstCapacity(5));
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton("reactiveStringRedisTemplate", redis);
        applicationContext.getBeanFactory().registerSingleton("redisRequestRateLimiterScript", script);
        applicationContext.refresh();
        redisRateLimiter.setApplicationContext(applicationContext);
        applicationContext.close();
        return redisRateLimiter;
    }
}
