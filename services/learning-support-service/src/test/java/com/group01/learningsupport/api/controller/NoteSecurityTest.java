package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.learningsupport.application.result.NoteResult;
import com.group01.learningsupport.application.result.PageResult;
import com.group01.learningsupport.application.query.PageQuery;
import com.group01.learningsupport.application.usecase.*;
import com.group01.learningsupport.domain.aggregate.Note;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.NoteSourceType;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "app.auth.external-jwt-issuer=urn:code-base:auth",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class NoteSecurityTest {
    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String INTERNAL_ISSUER = "urn:code-base:api-gateway";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateNoteUseCase createNoteUseCase;
    @MockBean
    private UpdateNoteUseCase updateNoteUseCase;
    @MockBean
    private GetNoteUseCase getNoteUseCase;
    @MockBean
    private ListNotesUseCase listNotesUseCase;
    @MockBean
    private DeleteNoteUseCase deleteNoteUseCase;

    @DynamicPropertySource
    static void hmacProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/learning-support/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Note\",\"body\":\"Body\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSubjectIsPassedToCreateNote() throws Exception {
        UUID userId = UUID.randomUUID();
        when(createNoteUseCase.execute(userId, "Note", "Body"))
                .thenReturn(NoteResult.from(Note.create(userId, "Note", "Body")));

        mockMvc.perform(post("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Note\",\"body\":\"Body\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.title").value("Note"))
                .andExpect(jsonPath("$.body").value("Body"));

        verify(createNoteUseCase).execute(userId, "Note", "Body");
    }

    @Test
    void createsSourcedNoteAndReturnsSource() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        when(createNoteUseCase.execute(userId, "Note", "Body", NoteSourceType.KNOWLEDGE_POINT, sourceId))
                .thenReturn(NoteResult.from(Note.create(userId, "Note", "Body",
                        NoteSourceType.KNOWLEDGE_POINT, sourceId)));

        mockMvc.perform(post("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Note\",\"body\":\"Body\",\"sourceType\":\"KNOWLEDGE_POINT\",\"sourceReferenceId\":\"" + sourceId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("KNOWLEDGE_POINT"))
                .andExpect(jsonPath("$.sourceReferenceId").value(sourceId.toString()));
        verify(createNoteUseCase).execute(userId, "Note", "Body", NoteSourceType.KNOWLEDGE_POINT, sourceId);
    }

    @Test
    void rejectsUnknownOrIncompleteSource() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = "Bearer " + signedToken(userId);
        for (String json : List.of(
                "{\"title\":\"Note\",\"body\":\"Body\",\"sourceType\":\"UNKNOWN\"}",
                "{\"title\":\"Note\",\"body\":\"Body\",\"sourceType\":\"TUTOR_SESSION\"}",
                "{\"title\":\"Note\",\"body\":\"Body\",\"sourceReferenceId\":\"" + UUID.randomUUID() + "\"}"
        )) {
            mockMvc.perform(post("/api/learning-support/notes")
                            .header(HttpHeaders.AUTHORIZATION, token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void listsBySourceTypeAndSourceReference() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        PageResult<NoteResult> emptyPage = new PageResult<>(List.of(), 0, 20, 0);
        when(listNotesUseCase.execute(eq(userId), eq(LibraryStatus.ACTIVE), any(PageQuery.class),
                eq(NoteSourceType.TUTOR_SESSION), eq(null))).thenReturn(emptyPage);
        when(listNotesUseCase.execute(eq(userId), eq(LibraryStatus.ACTIVE), any(PageQuery.class),
                eq(NoteSourceType.TUTOR_SESSION), eq(sourceId))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .param("sourceType", "TUTOR_SESSION"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .param("sourceType", "TUTOR_SESSION")
                        .param("sourceReferenceId", sourceId.toString()))
                .andExpect(status().isOk());

        verify(listNotesUseCase).execute(userId, LibraryStatus.ACTIVE, new PageQuery(0, 20),
                NoteSourceType.TUTOR_SESSION, null);
        verify(listNotesUseCase).execute(userId, LibraryStatus.ACTIVE, new PageQuery(0, 20),
                NoteSourceType.TUTOR_SESSION, sourceId);
    }

    @Test
    void rejectsSourceReferenceWithoutType() throws Exception {
        mockMvc.perform(get("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(UUID.randomUUID()))
                        .param("sourceReferenceId", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownSourceTypeFilter() throws Exception {
        mockMvc.perform(get("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(UUID.randomUUID()))
                        .param("sourceType", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    private static String signedToken(UUID subject) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(INTERNAL_ISSUER)
                .subject(subject.toString())
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("CUSTOMER"))
                .build();
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                new SecretKeySpec(Base64.getDecoder().decode(INTERNAL_SECRET), "HmacSHA256"))
                .build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
