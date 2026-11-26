package com.ieltspath.commonsecurity.authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(LearnerOnlyAuthorizationTest.MethodSecurityConfiguration.class)
class LearnerOnlyAuthorizationTest {
    @Autowired LearnerEndpoint endpoint;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void learnerCanUseLearnerOnlyFeature() {
        authenticate("CUSTOMER");

        assertThat(endpoint.read()).isEqualTo("learner data");
    }

    @Test
    void staffRolesCannotUseLearnerOnlyFeature() {
        authenticate("CONTENT_AUTHOR");
        assertThrows(AccessDeniedException.class, endpoint::read);

        authenticate("SALES_STAFF");
        assertThrows(AccessDeniedException.class, endpoint::read);
    }

    private static void authenticate(String role) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated("user", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
        @Bean LearnerEndpoint learnerEndpoint() { return new LearnerEndpoint(); }
    }

    @LearnerOnly
    static class LearnerEndpoint {
        public String read() { return "learner data"; }
    }
}
