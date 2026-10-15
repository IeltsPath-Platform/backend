package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.usecase.VocabularyReviewUseCase;
import com.ieltspath.library.application.result.VocabularyReviewResult;
import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularyReviewController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties={"spring.cloud.config.enabled=false","eureka.client.enabled=false","app.auth.internal-jwt-issuer=urn:test:gateway"})
class VocabularyReviewControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean VocabularyReviewUseCase reviews;
    @DynamicPropertySource static void key(DynamicPropertyRegistry r) {
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        r.add("app.auth.internal-jwt-secret",()->Base64.getEncoder().encodeToString(key));
    }
    @Test void anonymousStatisticsAreUnauthorized() throws Exception {
        mvc.perform(get("/api/learning-support/flashcards/statistics/vocabulary")).andExpect(status().isUnauthorized());
    }
    @Test void sourceContextUsesAuthenticatedSubjectIgnoringRawIdentityHeader() throws Exception {
        UUID user=UUID.randomUUID(),source=UUID.randomUUID();
        when(reviews.context(user,"VOCABULARY_REVIEW",source)).thenReturn(new VocabularyReviewResult.StudyContext(source,null));
        mvc.perform(get("/api/learning-support/flashcards/statistics/study-context")
                .param("sourceType","VOCABULARY_REVIEW").param("sourceId",source.toString())
                .header("X-User-Id",UUID.randomUUID().toString()).with(jwt().jwt(j->j.subject(user.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceId").value(source.toString()));
        verify(reviews).context(user,"VOCABULARY_REVIEW",source);
    }
    @Test void oversizedQueueAndMissingAnswerVersionAreRejected() throws Exception {
        var authentication=jwt().jwt(j->j.subject(UUID.randomUUID().toString()));
        mvc.perform(post("/api/learning-support/flashcards/vocabulary-review-sessions").with(authentication)
                .contentType("application/json").content("{\"requestId\":\""+UUID.randomUUID()+"\",\"limit\":51}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/learning-support/flashcards/vocabulary-review-sessions/"+UUID.randomUUID()+"/answers").with(authentication)
                .contentType("application/json").content("{\"requestId\":\""+UUID.randomUUID()+"\",\"vocabularySenseId\":\""+UUID.randomUUID()+"\",\"outcome\":\"REMEMBERED\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reviews);
    }
    @Test void validAnswerBindsSessionIdAndCallsUseCase() throws Exception {
        UUID user=UUID.randomUUID(),session=UUID.randomUUID(),request=UUID.randomUUID(),sense=UUID.randomUUID();
        when(reviews.answer(user,session,request,sense,0,VocabularyProgress.Outcome.REMEMBERED))
                .thenReturn(new VocabularyReviewResult.Progress(sense,VocabularyProgress.State.L1,1,
                        Instant.parse("2026-10-16T00:00:00Z"),true));
        mvc.perform(post("/api/learning-support/flashcards/vocabulary-review-sessions/{id}/answers",session)
                .with(jwt().jwt(j->j.subject(user.toString()))).contentType("application/json")
                .content("{\"requestId\":\""+request+"\",\"vocabularySenseId\":\""+sense+
                        "\",\"expectedVersion\":0,\"outcome\":\"REMEMBERED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vocabularySenseId").value(sense.toString()));
        verify(reviews).answer(user,session,request,sense,0,VocabularyProgress.Outcome.REMEMBERED);
    }
}
