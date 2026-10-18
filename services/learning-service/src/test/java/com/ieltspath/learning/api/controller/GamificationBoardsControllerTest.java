package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.config.CommonSecurityAutoConfiguration;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.BadgesUseCase;
import com.ieltspath.learning.application.usecase.GetLeagueUseCase;
import com.ieltspath.learning.application.usecase.GetQuestsUseCase;
import com.ieltspath.learning.domain.repository.BadgeRepository.Badge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The three learner boards over HTTP: today's quests, the badge shelf and the weekly league. */
@WebMvcTest(controllers = {QuestController.class, BadgeController.class, LeagueController.class}, properties = {
        "spring.cloud.config.enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"})
@Import(CommonSecurityAutoConfiguration.class)
class GamificationBoardsControllerTest {
    static final byte[] KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @Autowired MockMvc mvc;
    @MockitoBean CurrentUserProvider currentUser;
    @MockitoBean GetQuestsUseCase quests;
    @MockitoBean BadgesUseCase badges;
    @MockitoBean GetLeagueUseCase league;
    final UUID learner = UUID.randomUUID();

    @Test
    void questsListTodaysQuestsWithTheirProgressAndTheWeaknessTarget() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        UUID kp = UUID.randomUUID();
        when(quests.view(learner)).thenReturn(new GetQuestsUseCase.QuestsView(LocalDate.parse("2026-10-18"), 1, List.of(
                new GetQuestsUseCase.Item("ANSWERS", 10, 10, true, 10, 5, null),
                new GetQuestsUseCase.Item("WEAK_POINT", 3, 1, false, 15, 5,
                        new GetQuestsUseCase.Target(kp, "R60", "True/False/Not Given")))));

        mvc.perform(get("/api/learning/me/quests").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-18"))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.quests.length()").value(2))
                .andExpect(jsonPath("$.quests[0].completed").value(true))
                .andExpect(jsonPath("$.quests[1].target.code").value("R60"));
    }

    @Test
    void questsAndLeagueAreForTheLearnerOnly() throws Exception {
        mvc.perform(get("/api/learning/me/quests").with(user("staff").roles("EXAMINER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/learning/me/league").with(user("staff").roles("EXAMINER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/learning/me/league/history").with(user("staff").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/learning/me/badges").with(user("staff").roles("EXAMINER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void theBadgeShelfShowsHeldBadgesAndProgressOnTheOthers() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(badges.mine(learner)).thenReturn(List.of(
                new BadgesUseCase.Item("FIRST_STEP", "First step", "Answer a question", 5, true,
                        Instant.parse("2026-10-17T10:00:00Z"), null),
                new BadgesUseCase.Item("STREAK_7", "Seven days", "Seven days in a row", 15, false, null,
                        new BadgesUseCase.Progress(3, 7))));

        mvc.perform(get("/api/learning/me/badges").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].awarded").value(true))
                .andExpect(jsonPath("$[1].awarded").value(false))
                .andExpect(jsonPath("$[1].progress.current").value(3))
                .andExpect(jsonPath("$[1].progress.target").value(7));
    }

    @Test
    void anAdminSwitchesABadgeOffAndOnlyAnAdminCan() throws Exception {
        when(badges.setActive("STREAK_7", false))
                .thenReturn(new Badge("STREAK_7", "Seven days", "Seven days in a row", 15, false));

        mvc.perform(patch("/api/learning/admin/badges/STREAK_7").with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("STREAK_7"))
                .andExpect(jsonPath("$.active").value(false));
        verify(badges).setActive("STREAK_7", false);

        mvc.perform(patch("/api/learning/admin/badges/STREAK_7").with(user("learner").roles("CUSTOMER"))
                        .contentType("application/json").content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void switchingABadgeWithoutAnActiveFlagIsRefused() throws Exception {
        mvc.perform(patch("/api/learning/admin/badges/STREAK_7").with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void anUnknownBadgeIsNotFound() throws Exception {
        when(badges.setActive("NOPE", true))
                .thenThrow(new LearningRequestException(404, "BADGE_NOT_FOUND", "No badge with this code"));

        mvc.perform(patch("/api/learning/admin/badges/NOPE").with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content("{\"active\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BADGE_NOT_FOUND"));
    }

    @Test
    void theLeagueShowsTheTableAndTheZones() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(league.view(learner)).thenReturn(new GetLeagueUseCase.LeagueView("SILVER", LocalDate.parse("2026-10-18"),
                Instant.parse("2026-10-25T00:00:00Z"), true, List.of(
                        new GetLeagueUseCase.Entry(1, "An", 300, false, "PROMOTE"),
                        new GetLeagueUseCase.Entry(2, "You", 120, true, "SAFE")), 2, 7, 23));

        mvc.perform(get("/api/learning/me/league").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("SILVER"))
                .andExpect(jsonPath("$.joined").value(true))
                .andExpect(jsonPath("$.table[1].you").value(true))
                .andExpect(jsonPath("$.promoteTop").value(7))
                .andExpect(jsonPath("$.demoteFrom").value(23));
    }

    @Test
    void theLeagueHistoryTakesTheNumberOfWeeksAndDefaultsToEight() throws Exception {
        when(currentUser.requireUserId()).thenReturn(learner);
        when(league.history(learner, 8)).thenReturn(List.of(
                new GetLeagueUseCase.HistoryEntry(LocalDate.parse("2026-10-11"), "BRONZE", 410, 3, "PROMOTED")));
        when(league.history(learner, 2)).thenReturn(List.of());

        mvc.perform(get("/api/learning/me/league/history").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].outcome").value("PROMOTED"))
                .andExpect(jsonPath("$[0].finalRank").value(3));
        mvc.perform(get("/api/learning/me/league/history").param("weeks", "2").with(user("learner").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
