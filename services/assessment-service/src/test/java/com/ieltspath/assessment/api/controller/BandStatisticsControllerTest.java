package com.ieltspath.assessment.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.assessment.application.usecase.GetBandHistoryUseCase;
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
import java.time.LocalDate;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BandStatisticsController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties={"spring.cloud.config.enabled=false","eureka.client.enabled=false","app.auth.internal-jwt-issuer=urn:test:gateway"})
class BandStatisticsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GetBandHistoryUseCase history;
    @DynamicPropertySource static void key(DynamicPropertyRegistry r) {
        byte[] key=new byte[32];new java.security.SecureRandom().nextBytes(key);
        r.add("app.auth.internal-jwt-secret",()->Base64.getEncoder().encodeToString(key));
    }
    @Test void anonymousHistoryIsUnauthorized() throws Exception {
        mvc.perform(get("/api/assessments/me/statistics/bands").param("from","2026-10-01").param("to","2026-10-15"))
                .andExpect(status().isUnauthorized());
    }
    @Test void invalidDateIsBadRequestInsteadOfInternalError() throws Exception {
        mvc.perform(get("/api/assessments/me/statistics/bands").param("from","bad").param("to","2026-10-15")
                .with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isBadRequest());
        verifyNoInteractions(history);
    }
    @Test void validHistoryBindsDatesAndDefaultsAndCallsUseCase() throws Exception {
        UUID user=UUID.randomUUID();
        LocalDate from=LocalDate.parse("2026-10-01"),to=LocalDate.parse("2026-10-15");
        when(history.execute(user,from,to,"PLACEMENT","OVERALL",null,100,"Asia/Ho_Chi_Minh")).thenReturn(
                new GetBandHistoryUseCase.Result("AVAILABLE",java.time.Instant.parse("2026-10-15T00:00:00Z"),
                        from,"PLACEMENT","OVERALL",null,List.of(),null,null,null,null));
        mvc.perform(get("/api/assessments/me/statistics/bands").param("from",from.toString()).param("to",to.toString())
                .with(jwt().jwt(j->j.subject(user.toString())))).andExpect(status().isOk());
        verify(history).execute(user,from,to,"PLACEMENT","OVERALL",null,100,"Asia/Ho_Chi_Minh");
    }
    @Test void explicitTimezoneIsPassedToUseCase() throws Exception {
        UUID user=UUID.randomUUID();
        mvc.perform(get("/api/assessments/me/statistics/bands").param("from","2026-10-01").param("to","2026-10-15")
                .param("timezone","America/New_York").with(jwt().jwt(j->j.subject(user.toString()))))
                .andExpect(status().isOk());
        verify(history).execute(user,LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-15"),
                "PLACEMENT","OVERALL",null,100,"America/New_York");
    }
    @Test void invalidTimezoneIsBadRequest() throws Exception {
        UUID user=UUID.randomUUID();
        when(history.execute(user,LocalDate.parse("2026-10-01"),LocalDate.parse("2026-10-15"),
                "PLACEMENT","OVERALL",null,100,"Invalid/Zone")).thenThrow(new IllegalArgumentException("Invalid IANA timezone"));
        mvc.perform(get("/api/assessments/me/statistics/bands").param("from","2026-10-01").param("to","2026-10-15")
                .param("timezone","Invalid/Zone").with(jwt().jwt(j->j.subject(user.toString()))))
                .andExpect(status().isBadRequest());
    }
    @Test void contextPassesAuthenticatedOwner() throws Exception {
        UUID user=UUID.randomUUID(),source=UUID.randomUUID();
        when(history.context(user,"ASSESSMENT",source)).thenReturn(new GetBandHistoryUseCase.StudyContext(source,null));
        mvc.perform(get("/api/assessments/me/statistics/study-context").param("sourceType","ASSESSMENT").param("sourceId",source.toString())
                .with(jwt().jwt(j->j.subject(user.toString())))).andExpect(status().isOk());
        verify(history).context(user,"ASSESSMENT",source);
    }
}
