package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.domain.aggregate.Hearts;
import com.ieltspath.learning.domain.repository.HeartsRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartsServiceTest {
    private final HeartsRepository repository = mock(HeartsRepository.class);
    private final AccessClient access = mock(AccessClient.class);
    private final HeartsService service = new HeartsService(repository, access, 5, 7, 5);
    private final UUID user = UUID.randomUUID();

    @Test
    void aSubmissionThatLevelsUpAsksAccessForThePlanOnce() {
        Hearts stored = Hearts.start(user, 5, Instant.now());
        when(access.premium()).thenReturn(true);
        when(repository.find(user)).thenReturn(Optional.of(stored));

        service.requireHearts(user);
        service.lose(user, 2);
        service.refillOnLevelUp(user);

        verify(access, times(1)).premium();
        assertEquals(7, stored.max(), "the plan read by the first step applies to the whole submission");
        assertEquals(7, stored.hearts(), "the level-up refilled what the wrong answers took");
    }

    @Test
    void wrongAnswersTakeHeartsFromTheStoredMaximumWithoutAskingAccess() {
        Hearts stored = Hearts.start(user, 7, Instant.now());
        when(repository.find(user)).thenReturn(Optional.of(stored));

        HeartsService.Snapshot afterwards = service.lose(user, 3);

        assertEquals(4, afterwards.hearts());
        assertEquals(7, afterwards.maxHearts());
        verify(access, times(0)).premium();
    }

    @Test
    void aLearnerWithNoHeartsYetStartsFullWhenALevelUpComesFirst() {
        when(repository.find(user)).thenReturn(Optional.empty());

        service.refillOnLevelUp(user);

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(hearts -> hearts.hearts() == 5 && hearts.max() == 5));
        verify(access, times(0)).premium();
    }

    @Test
    void whenAccessCannotAnswerTheStoredMaximumIsKept() {
        Hearts stored = Hearts.start(user, 7, Instant.now());
        when(access.premium()).thenThrow(new AccessUnavailableException(503));
        when(repository.find(user)).thenReturn(Optional.of(stored));

        service.requireHearts(user);

        assertEquals(7, stored.max());
    }
}
