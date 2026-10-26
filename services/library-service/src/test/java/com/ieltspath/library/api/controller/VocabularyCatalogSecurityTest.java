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
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({VocabularyController.class, VocabularyCatalogController.class, AdminVocabularyController.class, AdminVocabularyCatalogController.class})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "app.security.public-endpoints[0].method=GET",
        "app.security.public-endpoints[0].patterns[0]=/api/content/vocabulary",
        "app.security.public-endpoints[0].patterns[1]=/api/content/vocabulary/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
        "app.security.public-endpoints[0].patterns[2]=/api/content/vocabulary/suggestions",
        "app.security.public-endpoints[0].patterns[3]=/api/content/vocabulary/topics",
        "app.security.public-endpoints[0].patterns[4]=/api/content/vocabulary/sets",
        "app.security.public-endpoints[0].patterns[5]=/api/content/vocabulary/mindmap",
        "app.security.public-endpoints[0].patterns[6]=/api/content/vocabulary/sets/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}",
        "app.security.public-endpoints[0].patterns[7]=/api/content/vocabulary/sets/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/words",
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
    @MockBean VocabularyCatalogUseCase catalog;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> SECRET);
    }

    @Test void catalogGetRoutesArePublicButAdjacentRoutesRemainProtected() throws Exception {
        UUID id = UUID.fromString("10000000-0000-4000-8000-000000000001");
        Instant now = Instant.parse("2026-10-26T00:00:00Z");
        when(catalog.browse("", "EN", null, null, 0, 20)).thenReturn(java.util.Map.of("items",List.of(),"page",0,"size",20,"total",0));
        when(catalog.detail(id)).thenReturn(java.util.Map.of("id",id,"lemma","word","senses",List.of()));
        mvc.perform(get("/api/content/vocabulary")).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/suggestions").param("q","wo")).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/topics")).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/sets/{id}",id)).andExpect(status().isOk());
        mvc.perform(get("/api/content/vocabulary/sets/{id}/words",id)).andExpect(status().isOk());
        mvc.perform(post("/api/content/vocabulary").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/vocabulary/search")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/vocabulary/senses").param("ids", id.toString())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/content/admin/vocabulary/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/content/admin/vocabulary/items/{id}",id)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void rejectsCatalogLimitsAndHidesInactiveDetails() throws Exception {
        UUID id = UUID.fromString("10000000-0000-4000-8000-000000000002");
        Instant now = Instant.parse("2026-10-26T00:00:00Z");
        mvc.perform(get("/api/content/vocabulary").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/content/vocabulary").param("size", "51")).andExpect(status().isBadRequest());
        when(catalog.detail(id)).thenThrow(new com.ieltspath.library.domain.exception.ResourceNotFoundException());
        mvc.perform(get("/api/content/vocabulary/{id}", id)).andExpect(status().isNotFound());
    }

    @Test void catalogAdminWriteRejectsCustomerRole() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/content/admin/vocabulary/items/{id}",UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION,"Bearer "+signedInternalToken("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"academicWordList\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test void catalogAdminWriteRejectsInvalidCefr() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/content/admin/vocabulary/senses/{id}",UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION,"Bearer "+signedInternalToken("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cefrLevel\":\"Z9\"}"))
                .andExpect(status().isBadRequest());
    }

    private String signedInternalToken(String role) {
        Instant now=Instant.now();
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(5))).claim("roles",List.of(role)).build();
        OctetSequenceKey key=new OctetSequenceKey.Builder(new SecretKeySpec(Base64.getDecoder().decode(SECRET),"HmacSHA256")).build();
        JwtEncoder encoder=new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }

}
