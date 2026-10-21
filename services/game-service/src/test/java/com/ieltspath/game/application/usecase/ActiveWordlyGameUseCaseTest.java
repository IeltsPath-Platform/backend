package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.ActiveWordlyGameRepository;
import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ActiveWordlyGameUseCaseTest {
    final ActiveWordlyGameRepository games = mock(ActiveWordlyGameRepository.class);
    final ChangeGameRoomMembershipUseCase membership = mock(ChangeGameRoomMembershipUseCase.class);
    final SoloGameplayUseCase gameplay = mock(SoloGameplayUseCase.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
    final ActiveWordlyGameUseCase useCase = new ActiveWordlyGameUseCase(games, membership, gameplay, clock);
    final UUID user = UUID.randomUUID(), room = UUID.randomUUID(), match = UUID.randomUUID();

    ActiveWordlyGameResult game(String roomStatus, UUID matchId, String playerStatus) {
        return new ActiveWordlyGameResult(room, "AB7K2Q", "MULTIPLAYER", roomStatus, matchId, playerStatus);
    }

    @Test void leavingAWaitingRoomUsesTheLobbyLifecycle() {
        when(games.find(user)).thenReturn(Optional.of(game("WAITING", null, null)));
        useCase.leave(user);
        verify(membership).leave(room, user);
        verifyNoInteractions(gameplay);
    }

    @Test void leavingWhilePlayingOrInReconnectGraceForfeitsTheMatch() {
        for (String status : new String[] {"ACTIVE", "DISCONNECTED"}) {
            reset(gameplay);
            when(games.find(user)).thenReturn(Optional.of(game("IN_MATCH", match, status)));
            useCase.leave(user);
            verify(gameplay).leave(match, user);
        }
        verifyNoInteractions(membership);
    }

    @Test void finishedOrEliminatedPlayersOnlyLeaveTheRoomWithoutForfeiting() {
        when(games.find(user)).thenReturn(Optional.of(game("IN_MATCH", match, "ELIMINATED")));
        useCase.leave(user);
        verify(games).leaveRunningMatchRoom(room, user, clock.instant());
        verifyNoInteractions(gameplay, membership);
    }

    @Test void leavingWithoutAnActiveGameIsANoOp() {
        when(games.find(user)).thenReturn(Optional.empty());
        useCase.leave(user);
        verify(games, never()).leaveRunningMatchRoom(any(), any(), any());
        verifyNoInteractions(gameplay, membership);
    }

    @Test void admissionLocksClosesIdleSoloRoomsThenRejectsAnActiveGame() {
        var admission = new WordlyRoomAdmission(games, clock);
        when(games.find(user)).thenReturn(Optional.of(game("WAITING", null, null)));
        var error = assertThrows(SoloGameplayException.class, () -> admission.requireNoActiveGame(user));
        assertEquals(409, error.status());
        assertEquals(WordlyRoomAdmission.ACTIVE_GAME_EXISTS, error.code());
        var order = inOrder(games);
        order.verify(games).lockUser(user);
        order.verify(games).closeIdleSoloRooms(user, clock.instant());
        order.verify(games).find(user);
    }

    @Test void admissionAllowsUsersWithoutAnActiveGame() {
        when(games.find(user)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> new WordlyRoomAdmission(games, clock).requireNoActiveGame(user));
    }
}
