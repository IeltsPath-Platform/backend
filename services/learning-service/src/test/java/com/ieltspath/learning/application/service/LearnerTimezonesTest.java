package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.GamificationRepository;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The learner's timezone decides the streak day and the daily XP cap. */
class LearnerTimezonesTest {
    private final GamificationRepository repository = mock(GamificationRepository.class);
    private final LearnerTimezones timezones = new LearnerTimezones(repository, "Asia/Ho_Chi_Minh");

    @Test
    void onlyIanaRegionIdsAreValid() {
        assertThat(LearnerTimezones.isValid("Asia/Ho_Chi_Minh")).isTrue();
        assertThat(LearnerTimezones.isValid("Europe/London")).isTrue();
        assertThat(LearnerTimezones.isValid("+07:00")).isFalse();
        assertThat(LearnerTimezones.isValid("ICT")).isFalse();
        assertThat(LearnerTimezones.isValid(null)).isFalse();
    }

    @Test
    void anInvalidTimezoneIsRefusedAndNotSaved() {
        UUID user = UUID.randomUUID();
        assertThatThrownBy(() -> timezones.save(user, "+07:00"))
                .isInstanceOf(LearningRequestException.class)
                .satisfies(error -> {
                    assertThat(((LearningRequestException) error).getStatus()).isEqualTo(400);
                    assertThat(((LearningRequestException) error).getCode()).isEqualTo("INVALID_TIMEZONE");
                });
        verify(repository, never()).saveTimezone(any(), any());
        timezones.save(user, "Europe/London");
        verify(repository).saveTimezone(user, "Europe/London");
    }

    @Test
    void aLearnerWhoHasNotChosenATimezoneGetsTheDefault() {
        UUID chosen = UUID.randomUUID(), unset = UUID.randomUUID();
        when(repository.timezone(unset)).thenReturn(Optional.empty());
        when(repository.timezone(chosen)).thenReturn(Optional.of("Europe/London"));
        when(repository.timezones(List.of(chosen, unset))).thenReturn(Map.of(chosen, "Europe/London"));

        assertThat(timezones.zoneOf(unset)).isEqualTo(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(timezones.zoneOf(chosen)).isEqualTo(ZoneId.of("Europe/London"));
        assertThat(timezones.zonesOf(List.of(chosen, unset))).containsOnly(
                Map.entry(chosen, ZoneId.of("Europe/London")), Map.entry(unset, ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}
