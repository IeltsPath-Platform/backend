package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.application.port.PackageAttemptStatsReader;
import com.ieltspath.assessment.application.usecase.GetPackageAttemptStatsUseCase;
import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PackageStatsController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:test:gateway"})
class PackageStatsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GetPackageAttemptStatsUseCase stats;

    @DynamicPropertySource
    static void key(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(key));
    }

    @Test
    void anonymousIsUnauthorizedAndALearnerIsForbidden() throws Exception {
        mvc.perform(get("/api/assessments/admin/package-stats")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/assessments/admin/package-stats")
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(stats);
    }

    @Test
    void anAuthorGetsTheStatsOfTheAskedPackages() throws Exception {
        UUID id = UUID.randomUUID();
        when(stats.execute(List.of(id))).thenReturn(List.of(
                new PackageAttemptStatsReader.PackageStats(id, 5, 4, new BigDecimal("6.5"))));

        mvc.perform(get("/api/assessments/admin/package-stats").param("packageIds", id.toString())
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CONTENT_AUTHOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packageId").value(id.toString()))
                .andExpect(jsonPath("$[0].started").value(5))
                .andExpect(jsonPath("$[0].submitted").value(4))
                .andExpect(jsonPath("$[0].averageBand").value(6.5));
    }
}
