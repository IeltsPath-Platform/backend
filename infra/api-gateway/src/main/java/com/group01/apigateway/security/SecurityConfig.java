package com.group01.apigateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group01.apigateway.error.GatewayErrorResponse;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableConfigurationProperties({PublicEndpointProperties.class, AuthProperties.class})
public class SecurityConfig {

    private static final Set<String> CANONICAL_ROLES = Set.of("ADMIN", "LEARNER");

    private static final List<HttpMethod> CORS_METHODS = List.of(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS
    );

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ObjectMapper objectMapper,
            AuthProperties authProperties,
            PublicEndpointProperties publicEndpointProperties
    ) {
        return http
                // Tat CSRF vi gateway dung stateless JWT, khong dung session/cookie form.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Bat CORS theo CorsWebFilter ben duoi.
                .cors(Customizer.withDefaults())

                // Tat Basic Auth vi he thong chi dung Bearer JWT.
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Tat form login vi gateway khong render trang dang nhap.
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Ghi JSON loi thong nhat khi token sai hoac khong du quyen.
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // Thieu token, token sai, token het han -> 401.
                        .authenticationEntryPoint((exchange, exception) ->
                                writeError(exchange, objectMapper, authProperties, HttpStatus.UNAUTHORIZED, "Unauthorized"))
                        // Token hop le nhung khong du quyen -> 403.
                        .accessDeniedHandler((exchange, exception) ->
                                writeError(exchange, objectMapper, authProperties, HttpStatus.FORBIDDEN, "Forbidden")))

                // Khong luu SecurityContext vao session; moi request tu xac thuc bang JWT.
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

                // Khai bao route nao public, route nao can JWT.
                .authorizeExchange(exchange -> {
                    // Cho phep preflight CORS.
                    exchange.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // Cho phep doc actuator health/info.
                    exchange.pathMatchers(HttpMethod.GET, "/actuator/**").permitAll();

                    // Cho phep cac endpoint public lay tu config, vi du login/register.
                    publicEndpointProperties.publicEndpoints().forEach(path ->
                            exchange.pathMatchers(path).permitAll());

                    // Cac route con lai bat buoc co JWT hop le.
                    exchange.anyExchange().authenticated();
                })

                // Bat OAuth2 Resource Server de Spring parse/verify Bearer JWT.
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Convert claim roles thanh GrantedAuthority cua Spring Security.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))

                // Build SecurityWebFilterChain cho WebFlux.
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(AuthProperties authProperties) {
        // Verify external JWT do user-service phat cho client bang external secret.
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(
                        HmacKeyFactory.secretKey(authProperties.externalJwtSecret(), "app.auth.external-jwt-secret"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Chi chap nhan dung issuer, subject UUID, va role nam trong danh sach he thong.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(authProperties.externalJwtIssuer()),
                subjectValidator(),
                canonicalRolesValidator()
        ));
        return decoder;
    }

    @Bean
    JwtEncoder internalJwtEncoder(AuthProperties authProperties) {
        // Ky internal JWT ngan han gui xuong service bang internal secret cua gateway.
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                HmacKeyFactory.secretKey(authProperties.internalJwtSecret(), "app.auth.internal-jwt-secret"))
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }

    @Bean
    CorsWebFilter corsWebFilter(AuthProperties authProperties) {
        // Cau hinh CORS cho frontend va localhost dev goi qua gateway.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(allowedOriginPatterns(authProperties));
        config.setAllowedMethods(CORS_METHODS.stream().map(HttpMethod::name).toList());
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        // Bien JWT da verify thanh Authentication de authorizeExchange dung duoc roles.
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject()));
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Spring Security can prefix ROLE_ de dung voi hasRole/role-based access.
        return claimRoles(jwt).stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private OAuth2TokenValidator<Jwt> canonicalRolesValidator() {
        // Tu choi token khong co role hoac role khong nam trong danh sach hop le.
        return jwt -> {
            Set<String> roles = claimRoles(jwt);
            return !roles.isEmpty() && CANONICAL_ROLES.containsAll(roles)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "The token contains an unsupported role", null));
        };
    }

    private OAuth2TokenValidator<Jwt> subjectValidator() {
        // Subject dai dien userId, bat buoc la UUID de tranh token subject tuy y.
        return jwt -> {
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The subject must be a UUID", null));
            }
            try {
                java.util.UUID.fromString(subject);
                return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The subject must be a UUID", null));
            }
        };
    }

    private Set<String> claimRoles(Jwt jwt) {
        // Lay roles tu claim "roles"; chap nhan ca array va string don.
        Set<String> roles = new LinkedHashSet<>();
        addRoles(roles, jwt.getClaim("roles"));
        return roles;
    }

    private void addRoles(Set<String> roles, Object value) {
        // Normalize roles ve String de validator va converter xu ly chung mot cach.
        if (value instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
            return;
        }
        if (value instanceof String role && !role.isBlank()) {
            roles.add(role);
        }
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            ObjectMapper objectMapper,
            AuthProperties authProperties,
            HttpStatus status,
            String message
    ) {
        // Tra ve JSON loi thay vi response mac dinh cua Spring Security.
        addCorsHeaders(exchange, authProperties);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse body = new GatewayErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getPath().value(),
                null
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException exception) {
            return Mono.error(exception);
        }
    }

    private void addCorsHeaders(ServerWebExchange exchange, AuthProperties authProperties) {
        // Them CORS vao response loi, neu khong browser co the chan body loi.
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin == null || origin.isBlank() || !isAllowedOrigin(origin, authProperties)) {
            return;
        }

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.setAccessControlAllowOrigin(origin);
        headers.setAccessControlAllowCredentials(true);
        headers.setAccessControlAllowMethods(CORS_METHODS);
        headers.setAccessControlAllowHeaders(List.of("*"));
        headers.setAccessControlExposeHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        headers.setAccessControlMaxAge(3600L);
        headers.setVary(List.of(
                HttpHeaders.ORIGIN,
                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS
        ));
    }

    private boolean isAllowedOrigin(String origin, AuthProperties authProperties) {
        // Production dung frontendOrigin; localhost/127.0.0.1 danh cho dev frontend.
        return origin.equals(authProperties.frontendOrigin())
                || origin.startsWith("http://localhost:")
                || origin.startsWith("http://127.0.0.1:");
    }

    private List<String> allowedOriginPatterns(AuthProperties authProperties) {
        // Pattern nay duoc CorsWebFilter dung cho request binh thuong.
        return List.of(authProperties.frontendOrigin(), "http://localhost:*", "http://127.0.0.1:*");
    }
}
