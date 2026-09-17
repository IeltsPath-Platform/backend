package com.group01.commonsecurity.config;

import com.group01.commonsecurity.currentuser.CurrentUserProvider;
import com.group01.commonsecurity.currentuser.SecurityContextCurrentUserProvider;
import com.group01.commonsecurity.jwt.HmacKeyFactory;
import com.group01.commonsecurity.jwt.InternalJwtAuthorities;
import com.group01.commonsecurity.jwt.InternalJwtValidators;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration
//Nó bật security ở cấp method, cho phép sử dụng các annotation như @PreAuthorize, @PostAuthorize, @PreFilter, @PostFilter
@EnableMethodSecurity
//Chỉ cấu hình security nếu ứng dụng là web application (servlet)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
//Chỉ cấu hình security nếu các class HttpSecurity và JwtDecoder có trong classpath
@ConditionalOnClass({HttpSecurity.class, JwtDecoder.class})
//Chỉ cấu hình security nếu property app.security.enabled = true hoặc không được định nghĩa
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
//Cho phép binding các property từ file cấu hình vào các class InternalJwtProperties và DownstreamSecurityProperties
@EnableConfigurationProperties({InternalJwtProperties.class, DownstreamSecurityProperties.class})
public class CommonSecurityAutoConfiguration {
    @Bean
    //Common library chỉ tạo SecurityFilterChain mặc định nếu application chưa tự định nghĩa một cái
    @ConditionalOnMissingBean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DownstreamSecurityProperties securityProperties,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    authorize.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    securityProperties.publicEndpoints().forEach(endpoint -> {
                        String[] patterns = endpoint.patterns().toArray(String[]::new);
                        if (patterns.length == 0) {
                            return;
                        }
                        if (endpoint.method() == null) {
                            authorize.requestMatchers(patterns).permitAll();
                            return;
                        }
                        authorize.requestMatchers(endpoint.method(), patterns).permitAll();
                    });
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder(InternalJwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(
                        HmacKeyFactory.secretKey(properties.internalJwtSecret(), "app.auth.internal-jwt-secret"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(InternalJwtValidators.internalJwtValidator(properties.internalJwtIssuer()));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> new JwtAuthenticationToken(
                jwt,
                InternalJwtAuthorities.extractAuthorities(jwt),
                jwt.getSubject());
    }

    @Bean
    @ConditionalOnMissingBean
    CurrentUserProvider currentUserProvider() {
        return new SecurityContextCurrentUserProvider();
    }
}
