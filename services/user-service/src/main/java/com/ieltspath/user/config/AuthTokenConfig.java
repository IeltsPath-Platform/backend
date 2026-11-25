package com.ieltspath.user.config;

import com.ieltspath.commonsecurity.jwt.HmacKeyFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties({AuthTokenProperties.class, AuthCookieProperties.class, AuthPasswordProperties.class})
public class AuthTokenConfig {
    @Bean
    @Primary
    PasswordEncoder passwordEncoder(AuthPasswordProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptCost());
    }

    @Bean
    @Qualifier("legacyPasswordEncoder")
    PasswordEncoder legacyPasswordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    JwtEncoder jwtEncoder(AuthTokenProperties properties) {
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                HmacKeyFactory.secretKey(properties.externalJwtSecret(), "app.auth.external-jwt-secret"))
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
    }
}
