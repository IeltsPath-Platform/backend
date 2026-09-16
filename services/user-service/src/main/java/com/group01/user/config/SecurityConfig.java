package com.group01.user.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthTokenProperties.class)
public class SecurityConfig {
    private static final Set<String> CANONICAL_ROLES = Set.of("ADMIN", "LEARNER");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Tat CSRF vi service dung stateless JWT, khong dung session/cookie form.
                .csrf(AbstractHttpConfigurer::disable)
                // Tat Basic Auth vi service chi nhan Bearer JWT tu gateway.
                .httpBasic(AbstractHttpConfigurer::disable)
                // Tat form login vi login duoc xu ly bang API /auth/login.
                .formLogin(AbstractHttpConfigurer::disable)
                // Khong tao HTTP session; moi request tu xac thuc bang JWT.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Khai bao endpoint public; cac endpoint con lai can internal JWT hop le.
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phep preflight CORS.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Cho phep health/info de monitoring khong can token.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Cac API auth public: login, refresh, logout, register.
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh", "/auth/logout", "/api/users/register").permitAll()
                        // Cac request con lai phai authenticated.
                        .anyRequest().authenticated())
                // Bat Resource Server de Spring verify Bearer JWT.
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Convert claim roles thanh GrantedAuthority.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt hash password nguoi dung truoc khi luu database.
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(AuthTokenProperties properties) {
        // Ky external JWT tra ve client sau login bang external secret.
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                HmacKeyFactory.secretKey(properties.externalJwtSecret(), "app.auth.external-jwt-secret"))
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder(AuthTokenProperties properties) {
        // Verify internal JWT do API Gateway ky bang internal secret.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        HmacKeyFactory.secretKey(properties.internalJwtSecret(), "app.auth.internal-jwt-secret"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Chi chap nhan dung issuer, subject UUID, va role hop le.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.internalJwtIssuer()),
                subjectValidator(),
                canonicalRolesValidator()
        ));
        return decoder;
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        // Bien JWT da verify thanh Authentication de @PreAuthorize/authorize request dung roles.
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
    }

    private OAuth2TokenValidator<Jwt> subjectValidator() {
        // Subject dai dien userId, bat buoc la UUID.
        return jwt -> {
            String subject = jwt.getSubject();
            if (subject == null || subject.isBlank()) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The subject must be a UUID", null));
            }
            try {
                UUID.fromString(subject);
                return OAuth2TokenValidatorResult.success();
            } catch (IllegalArgumentException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "The subject must be a UUID", null));
            }
        };
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

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Spring Security can prefix ROLE_ de dung voi hasRole/role-based access.
        return claimRoles(jwt).stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private Set<String> claimRoles(Jwt jwt) {
        // Lay roles tu claim "roles"; chap nhan ca array va string don.
        Set<String> roles = new LinkedHashSet<>();
        Object value = jwt.getClaim("roles");
        if (value instanceof List<?> list) {
            list.stream().map(String::valueOf).forEach(roles::add);
        } else if (value instanceof String role && !role.isBlank()) {
            roles.add(role);
        }
        return roles;
    }
}
