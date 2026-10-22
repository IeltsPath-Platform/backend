package com.ieltspath.game.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ieltspath.game.application.port.OutboxWriter;
import com.ieltspath.game.application.port.WordlyMatchStore;
import com.ieltspath.game.application.port.WordlyWordProvider;
import com.ieltspath.game.application.result.GameMatchResult;
import com.ieltspath.game.domain.aggregate.GameRoom;
import com.ieltspath.game.domain.aggregate.RocketRound;
import com.ieltspath.game.domain.repository.GameMatchPlayerRepository;
import com.ieltspath.game.domain.repository.GameMatchRepository;
import com.ieltspath.game.domain.repository.GameRoomRepository;
import com.ieltspath.game.domain.repository.GameSessionRepository;
import com.ieltspath.game.domain.vo.RocketOption;
import com.ieltspath.game.domain.vo.RocketRoomSettings;
import com.ieltspath.game.domain.vo.RocketWord;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

class StartRocketMatchUseCaseTest {
    private final List<RocketWord> bank = IntStream.rangeClosed(1, 12)
            .mapToObj(i -> new RocketWord(UUID.randomUUID(), "word" + i, "nghĩa " + i)).toList();

    @Test
    void preparesDistinctQuestionsWithValidOptionsRepeatablyPerMatch() {
        UUID match = UUID.randomUUID();
        var rounds = StartRocketMatchUseCase.prepareRounds(match, RocketRoomSettings.solo(10), bank);
        assertEquals(10, rounds.size());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), rounds.stream().map(RocketRound::sequence).toList());
        assertEquals(10, rounds.stream().map(RocketRound::meaning).distinct().count());
        for (RocketRound round : rounds) {
            assertEquals(RocketRound.Status.PENDING, round.status());
            String answer = round.options().stream().filter(o -> o.optionId().equals(round.correctOptionId()))
                    .map(RocketOption::word).findFirst().orElseThrow();
            String meaning = bank.stream().filter(w -> w.word().equals(answer)).findFirst().orElseThrow().meaning();
            assertEquals(meaning, round.meaning());
        }
        var again = StartRocketMatchUseCase.prepareRounds(match, RocketRoomSettings.solo(10), bank);
        assertEquals(rounds.stream().map(RocketRound::meaning).toList(), again.stream().map(RocketRound::meaning).toList());
        assertEquals(rounds.stream().map(RocketRound::options).toList(), again.stream().map(RocketRound::options).toList());
    }

    @Test
    void needsEnoughVocabularyForEveryQuestion() {
        assertThrows(IllegalStateException.class,
                () -> StartRocketMatchUseCase.prepareRounds(UUID.randomUUID(), RocketRoomSettings.solo(13), bank));
        assertDoesNotThrow(() -> StartRocketMatchUseCase.prepareRounds(UUID.randomUUID(), RocketRoomSettings.solo(12), bank));
    }

    @Test
    void roomStartHandsRocketRoomsToTheRocketStarter() {
        var rooms = mock(GameRoomRepository.class);
        var rocket = mock(StartRocketMatchUseCase.class);
        UUID host = UUID.randomUUID();
        GameRoom room = GameRoom.open(UUID.randomUUID(), "ABC123", host, RocketRoomSettings.solo(6), Instant.now());
        room.setReady(host, true, Instant.now());
        when(rooms.findById(room.id())).thenReturn(Optional.of(room));
        var expected = new GameMatchResult(UUID.randomUUID(), room.id(), null, "IN_PROGRESS", Instant.now(), List.of());
        when(rocket.execute(any(), any())).thenReturn(expected);
        var start = new StartGameMatchUseCase(rooms, mock(GameMatchRepository.class), mock(GameMatchPlayerRepository.class),
                mock(GameSessionRepository.class), mock(OutboxWriter.class), mock(ApplicationEventPublisher.class),
                mock(WordlyWordProvider.class), mock(WordlyMatchStore.class), mock(PlatformTransactionManager.class),
                Clock.systemUTC(), rocket);
        assertSame(expected, start.execute(room.id(), host));
        verify(rocket).execute(room, host);
        assertEquals(Set.of("ROCKET"), Set.of(room.gameType()));
    }
}
