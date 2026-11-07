package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.library.application.exception.StudyRequestException;
import com.ieltspath.library.application.result.StudyGamificationResult.*;
import com.ieltspath.library.application.usecase.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VocabularyStudyMeController.class)
@ImportAutoConfiguration(CommonSecurityAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class StudyMeSecurityTest {
    private static final byte[] KEY = new byte[32];
    private static final UUID USER = UUID.randomUUID(), REQUEST = UUID.randomUUID();
    private static final String ROOT = "/api/learning-support/vocabulary-study/me";
    private static final HeartsView HEARTS = new HeartsView(5, 5, null, 5);
    private static final StreakView STREAK = new StreakView(3, 7, LocalDate.of(2026, 11, 7), true,
            false, null, "QUALIFIED_TODAY", 0, "Asia/Ho_Chi_Minh");
    static { new SecureRandom().nextBytes(KEY); }

    @Autowired MockMvc mvc;
    @MockitoBean GetStudySummaryUseCase summary;
    @MockitoBean UpdateStudySettingsUseCase settings;
    @MockitoBean RefillStudyHeartsUseCase refill;
    @MockitoBean BuyStudyFreezeUseCase freeze;

    @DynamicPropertySource static void key(DynamicPropertyRegistry properties) {
        properties.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Test
    void personalEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get(ROOT + "/summary")).andExpect(status().isUnauthorized());
        mvc.perform(put(ROOT + "/settings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        for (String path : List.of("/hearts/refill", "/streak/freeze")) {
            mvc.perform(post(ROOT + path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(summary, settings, refill, freeze);
    }

    @Test
    void summaryUsesTokenIdentityAndPreservesTheNestedContract() throws Exception {
        when(summary.execute(USER)).thenReturn(new Summary(new XpView(530, 2, 1000, 470), STREAK, HEARTS));
        mvc.perform(get(ROOT + "/summary").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp.totalXp").value(530))
                .andExpect(jsonPath("$.xp.level").value(2))
                .andExpect(jsonPath("$.xp.nextLevelAt").value(1000))
                .andExpect(jsonPath("$.xp.xpToNextLevel").value(470))
                .andExpect(jsonPath("$.hearts.hearts").value(5))
                .andExpect(jsonPath("$.hearts.maxHearts").value(5))
                .andExpect(jsonPath("$.hearts.nextHeartAt").isEmpty())
                .andExpect(jsonPath("$.streak.lastQualifiedDate").value("2026-11-07"))
                .andExpect(jsonPath("$.streak.qualifiedToday").value(true))
                .andExpect(jsonPath("$.streak.freezeBlockedReason").value("QUALIFIED_TODAY"));
        verify(summary).execute(USER);
    }

    @Test
    void settingsUsesOnlyTokenIdentityAndReturnsTheTimezone() throws Exception {
        when(settings.execute(USER, "Asia/Ho_Chi_Minh")).thenReturn(new Settings("Asia/Ho_Chi_Minh"));
        mvc.perform(put(ROOT + "/settings").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timezone\":\"Asia/Ho_Chi_Minh\",\"userId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timezone").value("Asia/Ho_Chi_Minh"));
        verify(settings).execute(USER, "Asia/Ho_Chi_Minh");
    }

    @Test
    void blankTimezoneIsRejectedAndInvalidZoneHasBusinessCode() throws Exception {
        for (String body : List.of("{}", "{\"timezone\":\" \"}")) {
            mvc.perform(put(ROOT + "/settings").header(HttpHeaders.AUTHORIZATION, token())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(settings);
        when(settings.execute(USER, "invalid/zone")).thenThrow(new StudyRequestException(
                HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE", "Múi giờ không hợp lệ.", Map.of()));
        mvc.perform(put(ROOT + "/settings").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"timezone\":\"invalid/zone\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
    }

    @Test
    void purchasesForwardRequestIdAndReturnOnlyTheirEffect() throws Exception {
        when(refill.execute(USER, REQUEST)).thenReturn(new PurchaseResult(HEARTS, null, 5, 95));
        when(freeze.execute(USER, REQUEST)).thenReturn(new PurchaseResult(null, STREAK, 10, 85));
        String body = "{\"requestId\":\"" + REQUEST + "\",\"userId\":\"" + UUID.randomUUID() + "\"}";
        mvc.perform(post(ROOT + "/hearts/refill").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hearts.maxHearts").value(5))
                .andExpect(jsonPath("$.streak").doesNotExist())
                .andExpect(jsonPath("$.pointsSpent").value(5)).andExpect(jsonPath("$.pointsBalanceAfter").value(95));
        mvc.perform(post(ROOT + "/streak/freeze").header(HttpHeaders.AUTHORIZATION, token())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.streak.currentDays").value(3))
                .andExpect(jsonPath("$.hearts").doesNotExist())
                .andExpect(jsonPath("$.pointsSpent").value(10)).andExpect(jsonPath("$.pointsBalanceAfter").value(85));
        verify(refill).execute(USER, REQUEST);
        verify(freeze).execute(USER, REQUEST);
    }

    @Test
    void malformedPurchaseRequestsNeverReachTheUseCases() throws Exception {
        for (String path : List.of("/hearts/refill", "/streak/freeze")) {
            for (String body : List.of("{}", "{\"requestId\":null}", "{\"requestId\":\"invalid\"}")) {
                mvc.perform(post(ROOT + path).header(HttpHeaders.AUTHORIZATION, token())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(refill, freeze);
    }

    @Test
    void purchaseBusinessFailuresKeepTheirStatusCodeAndDetails() throws Exception {
        for (var failure : List.of(
                new StudyRequestException(HttpStatus.CONFLICT, "HEARTS_FULL", "Đã đủ tim.", Map.of()),
                new StudyRequestException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_POINTS", "Không đủ Points.", Map.of()),
                new StudyRequestException(HttpStatus.SERVICE_UNAVAILABLE, "ACCESS_UNAVAILABLE", "Vui lòng thử lại.", Map.of("retry", "true")))) {
            doThrow(failure).when(refill).execute(USER, REQUEST);
            mvc.perform(post(ROOT + "/hearts/refill").header(HttpHeaders.AUTHORIZATION, token())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"requestId\":\"" + REQUEST + "\"}"))
                    .andExpect(status().is(failure.status().value()))
                    .andExpect(jsonPath("$.code").value(failure.code()));
        }
        verify(refill, times(3)).execute(USER, REQUEST);
    }

    private static String token() {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("urn:code-base:api-gateway").subject(USER.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(120)).claim("roles", List.of("CUSTOMER")).build();
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(KEY, "HmacSHA256")));
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
