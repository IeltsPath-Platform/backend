package com.group01.learningsupport.api.controller;

import com.group01.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.group01.learningsupport.application.usecase.CreateNoteUseCase;
import com.group01.learningsupport.application.usecase.DeleteNoteUseCase;
import com.group01.learningsupport.application.usecase.GetNoteUseCase;
import com.group01.learningsupport.application.usecase.ListNotesUseCase;
import com.group01.learningsupport.application.usecase.UpdateNoteUseCase;
import com.group01.learningsupport.domain.aggregate.Note;
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
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        when(createNoteUseCase.execute(userId, "Note", "Body")).thenReturn(Note.create(userId, "Note", "Body"));

        mockMvc.perform(post("/api/learning-support/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Note\",\"body\":\"Body\"}"))
                .andExpect(status().isCreated());

        verify(createNoteUseCase).execute(userId, "Note", "Body");
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
