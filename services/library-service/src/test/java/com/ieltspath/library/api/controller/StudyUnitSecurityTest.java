package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.result.StudyContentResult.*;
import com.ieltspath.library.application.usecase.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({StudyUnitAuthoringController.class, VocabularyStudyContentController.class})
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class StudyUnitSecurityTest {
    private static final byte[] KEY = new byte[32];
    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID UNIT = UUID.randomUUID();
    private static final String AUTHOR = "/api/content/admin/vocabulary";
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired MockMvc mvc;
    @MockitoBean CreateStudyUnitUseCase create;
    @MockitoBean UpdateStudyUnitUseCase update;
    @MockitoBean ReorderStudyUnitsUseCase reorder;
    @MockitoBean ValidateStudyUnitUseCase validate;
    @MockitoBean PublishStudyUnitUseCase publish;
    @MockitoBean ArchiveStudyUnitUseCase archive;
    @MockitoBean ListAuthoringStudyUnitsUseCase list;
    @MockitoBean ListTopicStudyUnitsUseCase topicUnits;
    @MockitoBean GetStudyUnitContentUseCase content;

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("infra/config-server/config-repo/library-service.yaml"))) {
            root = root.getParent();
        }
        if (root == null) throw new IllegalStateException("Cannot locate the library security configuration");
        var sources = new YamlPropertySourceLoader().load("library", new FileSystemResource(
                root.resolve("infra/config-server/config-repo/library-service.yaml")));
        for (var source : sources) {
            for (var name : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                if (name.startsWith("app.security.public-endpoints")) {
                    registry.add(name, () -> source.getProperty(name));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "ADMIN", "EXAMINER", "SALES_STAFF"})
    void authoringRequiresContentAuthorForEveryOperation(String role) throws Exception {
        for (var request : authoringRequests()) {
            mvc.perform(request.header(HttpHeaders.AUTHORIZATION, token(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(create, update, reorder, validate, publish, archive, list);
    }

    @Test
    void contentAuthorCanUseAllAuthoringOperationsAndIdentityComesFromToken() throws Exception {
        for (var request : authoringRequests()) {
            mvc.perform(request.header(HttpHeaders.AUTHORIZATION, token("CONTENT_AUTHOR")))
                    .andExpect(status().is2xxSuccessful());
        }
        verify(create).execute(eq(TOPIC), eq(USER), any());
        verify(update).execute(eq(UNIT), eq(USER), any());
        verify(reorder).execute(TOPIC, List.of(UNIT));
        verify(validate).execute(UNIT);
        verify(publish).execute(UNIT);
        verify(archive).execute(UNIT);
        verify(list).execute(TOPIC);
    }

    @Test
    void publishedUnitReadsUseTheRealPublicAllowlistAndRetainNullableMedia() throws Exception {
        var summary = new UnitSummary(UNIT, TOPIC, 2, "Trong lớp học", null, 1,
                "LEARN", 4, 3, List.of("CHOICE", "MATCH", "FILL"));
        when(topicUnits.execute(TOPIC)).thenReturn(new TopicUnits(TOPIC, 7, 1, 4, List.of(summary)));
        var word = new Word(UUID.randomUUID(), UUID.randomUUID(), 1, "homework", "NOUN", "bài tập",
                null, null, null, null, null, null, null, null);
        when(content.execute(UNIT)).thenReturn(new UnitContent(summary, "Học từ trong lớp.", List.of(word)));

        mvc.perform(get("/api/content/vocabulary/topics/{id}/units", TOPIC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumVersion").value(7))
                .andExpect(jsonPath("$.items[0].unitId").value(UNIT.toString()));
        mvc.perform(get("/api/content/vocabulary/units/{id}", UNIT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit.revision").value(2))
                .andExpect(jsonPath("$.words[0].audioUkUrl").value(nullValue()))
                .andExpect(jsonPath("$.words[0].audioUsUrl").value(nullValue()))
                .andExpect(jsonPath("$.words[0].imageUrl").value(nullValue()));
    }

    @Test
    void personalStudyAndAuthoringRoutesRequireAuthentication() throws Exception {
        for (var request : authoringRequests()) mvc.perform(request).andExpect(status().isUnauthorized());
        for (String path : List.of("/progress", "/attempts/" + UNIT, "/me/summary")) {
            mvc.perform(get("/api/learning-support/vocabulary-study" + path))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/learning-support/vocabulary-study/units/{id}/attempts", UNIT)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(create, update, reorder, validate, publish, archive, list, topicUnits, content);
    }

    @Test
    void unpublishedArchivedAndMissingContentReturnTheCodedErrorContract() throws Exception {
        when(content.execute(UNIT)).thenThrow(StudyRequestException.missing("VOCAB_UNIT_NOT_FOUND"));
        mvc.perform(get("/api/content/vocabulary/units/{id}", UNIT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("VOCAB_UNIT_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/content/vocabulary/units/" + UNIT));
    }

    @Test
    void publishValidationUsesStringDetailsAndBusinessCode() throws Exception {
        when(publish.execute(UNIT)).thenThrow(new StudyRequestException(HttpStatus.UNPROCESSABLE_ENTITY,
                "VOCAB_UNIT_INVALID", "Bài học chưa hợp lệ.", Map.of("errors[0]", "TOO_FEW_WORDS")));
        mvc.perform(post(AUTHOR + "/study-units/{id}/publish", UNIT)
                        .header(HttpHeaders.AUTHORIZATION, token("CONTENT_AUTHOR")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VOCAB_UNIT_INVALID"))
                .andExpect(jsonPath("$.details['errors[0]']").value("TOO_FEW_WORDS"));
    }

    @Test
    void authoringRejectsInvalidBodyBeforeCallingUseCase() throws Exception {
        mvc.perform(post(AUTHOR + "/topics/{id}/study-units", TOPIC)
                        .header(HttpHeaders.AUTHORIZATION, token("CONTENT_AUTHOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"kind\":\"LEARN\",\"introductionVi\":\"\",\"senseIds\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(create);
    }

    private static List<MockHttpServletRequestBuilder> authoringRequests() {
        String body = """
                {"title":"Trong lớp học","kind":"LEARN","introductionVi":"Học từ trong lớp.",
                 "senseIds":["%s","%s","%s","%s"]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        return List.of(
                get(AUTHOR + "/topics/{id}/study-units", TOPIC),
                post(AUTHOR + "/topics/{id}/study-units", TOPIC).contentType(MediaType.APPLICATION_JSON).content(body),
                put(AUTHOR + "/study-units/{id}", UNIT).contentType(MediaType.APPLICATION_JSON).content(body),
                put(AUTHOR + "/topics/{id}/study-units/order", TOPIC).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitIds\":[\"" + UNIT + "\"]}"),
                get(AUTHOR + "/study-units/{id}/validation", UNIT),
                post(AUTHOR + "/study-units/{id}/publish", UNIT),
                post(AUTHOR + "/study-units/{id}/archive", UNIT));
    }

    private static String token(String role) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(USER.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(120)).claim("roles", List.of(role)).build();
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(KEY, "HmacSHA256")));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
