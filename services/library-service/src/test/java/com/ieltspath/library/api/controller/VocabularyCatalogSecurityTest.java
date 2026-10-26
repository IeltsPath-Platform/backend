package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({VocabularyController.class, AdminVocabularyController.class})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.security.public-endpoints[0].method=GET",
        "app.security.public-endpoints[0].patterns[0]=/api/content/vocabulary",
        "app.security.public-endpoints[0].patterns[1]=/api/content/vocabulary/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false"
})
class VocabularyCatalogSecurityTest {
    private static final String SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    @Autowired MockMvc mvc;
    @MockBean SearchVocabularyUseCase search;
    @MockBean GetVocabularyDetailUseCase detail;
    @MockBean GetVocabularySensesUseCase senses;
    @MockBean CreateVocabularyItemUseCase create;
    @MockBean AddVocabularySenseUseCase addSense;
    @MockBean BrowseVocabularyUseCase browse;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> SECRET);
    }

    @Test void catalogGetRoutesArePublicButAdjacentRoutesRemainProtected() throws Exception {
        UUID id = UUID.fromString("10000000-0000-4000-8000-000000000001");
        Instant now = Instant.parse("2026-10-26T00:00:00Z");
        when(browse.execute("", 0, 20)).thenReturn(new PageResult<>(List.of(), 0, 20, 0));
        when(detail.execute(id)).thenReturn(new VocabularyItemResult(id, "word", "word", null, null,
                ContentStatus.ACTIVE, now, now, List.of()));
        mvc.perform(get("/api/content/vocabulary")).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/{id}", id)).andExpect(status().isOk());
        mvc.perform(post("/api/content/vocabulary").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/vocabulary/search")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/vocabulary/senses").param("ids", id.toString())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/admin/vocabulary/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test void rejectsCatalogLimitsAndHidesInactiveDetails() throws Exception {
        UUID id = UUID.fromString("10000000-0000-4000-8000-000000000002");
        Instant now = Instant.parse("2026-10-26T00:00:00Z");
        mvc.perform(get("/api/content/vocabulary").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary").param("size", "51")).andExpect(status().isBadRequest());
        when(detail.execute(id)).thenReturn(new VocabularyItemResult(id, "old", "old", null, null,
                ContentStatus.INACTIVE, now, now, List.of()));
        mvc.perform(get("/api/content/vocabulary/{id}", id)).andExpect(status().isNotFound());
    }

}
